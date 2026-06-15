package com.fksoft.domain.booking;

import com.fksoft.domain.payment.PaymentGateway;
import com.fksoft.domain.payment.RefundRequest;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a settled payment to its reservation (SPEC-0016), reacting to gateway events. On success:
 * AWAITING_PAYMENT → CONFIRMED, seats HELD → SOLD, one ticket per seat. On failure: → CANCELLED,
 * seats HELD → FREE. A success for a no-longer-AWAITING_PAYMENT reservation (late, e.g. after
 * expiry) triggers an automatic refund and a WARN, with no state change. Runs in its own
 * transaction (REQUIRES_NEW) after the webhook commits.
 */
@Service
class ReservationConfirmer {

    private static final Logger log = LoggerFactory.getLogger(ReservationConfirmer.class);

    private final ReservationRepository reservations;
    private final ScreeningSeatRepository screeningSeats;
    private final TicketIssuer ticketIssuer;
    private final PaymentGateway paymentGateway;
    private final ConfirmationPublisher publisher;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    ReservationConfirmer(
            ReservationRepository reservations,
            ScreeningSeatRepository screeningSeats,
            TicketIssuer ticketIssuer,
            PaymentGateway paymentGateway,
            ConfirmationPublisher publisher,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.reservations = reservations;
        this.screeningSeats = screeningSeats;
        this.ticketIssuer = ticketIssuer;
        this.paymentGateway = paymentGateway;
        this.publisher = publisher;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onPaymentSucceeded(UUID reservationId, int amountCents) {
        var reservation = reservations.findById(reservationId).orElse(null);
        if (reservation == null) {
            return;
        }
        if (!reservation.isAwaitingPayment()) {
            paymentGateway.requestRefund(new RefundRequest(reservationId, amountCents));
            meterRegistry.counter("late_success_refunds_total").increment();
            log.warn("payment.late-success-refunded reservationId={} status={}", reservationId, reservation.status());
            return;
        }
        reservation.confirm();
        transition(reservation, ScreeningSeat::sell);
        var tickets = ticketIssuer.issue(reservation, clock.instant());
        publisher.publishConfirmed(reservation, tickets);
        meterRegistry.counter("reservations_confirmed_total").increment();
        log.info("reservation confirmed reservationId={} tickets={}", reservationId, tickets.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onPaymentFailed(UUID reservationId) {
        var reservation = reservations.findById(reservationId).orElse(null);
        if (reservation == null || !reservation.isAwaitingPayment()) {
            return;
        }
        reservation.cancel();
        transition(reservation, ScreeningSeat::release);
        publisher.publishFailed(reservation);
        meterRegistry.counter("reservations_payment_failed_total").increment();
        log.info("reservation payment failed reservationId={}", reservationId);
    }

    private void transition(Reservation reservation, java.util.function.Consumer<ScreeningSeat> transition) {
        var screeningSeatIds = reservation.seats().stream()
                .map(ReservationSeat::screeningSeatId)
                .toList();
        screeningSeats.findAllById(screeningSeatIds).forEach(transition);
    }
}
