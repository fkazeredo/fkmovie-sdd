package com.fksoft.domain.booking;

import com.fksoft.domain.payment.PaymentGateway;
import com.fksoft.domain.payment.PaymentRequest;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Starts the purchase of a reservation (SPEC-0016): the owner confirms a PENDING reservation, which
 * transitions it to AWAITING_PAYMENT, sets the payment deadline and requests an async charge via the
 * gateway port. Seats stay HELD while awaiting payment; the webhook (0015) later confirms or cancels.
 * Re-confirming an AWAITING_PAYMENT reservation is idempotent (no second charge).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationConfirmationService {

    private final ReservationRepository reservations;
    private final PaymentGateway paymentGateway;
    private final BookingProperties properties;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    /** Confirms a reservation and starts its payment (SPEC-0016); owner only, idempotent. */
    @Transactional
    public ReservationConfirmationView confirm(UUID reservationId, UUID callerId) {
        var reservation = reservations.findById(reservationId).orElseThrow(ReservationNotFoundException::new);
        if (!reservation.userId().equals(callerId)) {
            throw new ReservationAccessDeniedException();
        }
        return switch (reservation.status()) {
            case AWAITING_PAYMENT -> ReservationConfirmationView.of(reservation); // idempotent: same payment
            case CONFIRMED -> throw new AlreadyConfirmedException();
            case CANCELLED -> throw new ReservationCancelledException();
            case EXPIRED -> throw new ReservationExpiredException();
            case PENDING -> startPayment(reservation);
        };
    }

    private ReservationConfirmationView startPayment(Reservation reservation) {
        var now = clock.instant();
        if (reservation.isExpired(now)) {
            throw new ReservationExpiredException();
        }
        var deadline = now.plus(Duration.ofMinutes(properties.paymentDeadlineMinutes()));
        var payment = paymentGateway.request(PaymentRequest.of(reservation.id(), reservation.totalCents()));
        reservation.awaitPayment(payment.paymentId(), deadline);
        meterRegistry.counter("reservations_confirm_requested_total").increment();
        log.info("reservation confirm requested reservationId={} paymentId={}", reservation.id(), payment.paymentId());
        return ReservationConfirmationView.of(reservation);
    }
}
