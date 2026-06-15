package com.fksoft.domain.booking;

import com.fksoft.domain.payment.PaymentGateway;
import com.fksoft.domain.payment.RefundRequest;
import com.fksoft.domain.screening.ScreeningCatalog;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancels a customer's own reservation (SPEC-0018). PENDING/AWAITING_PAYMENT release their HELD seats
 * with no refund (a late payment success is refunded by 0016). CONFIRMED is cancellable only while at
 * least the cancellation window remains before the session: it cancels the tickets, returns the SOLD
 * seats to inventory (the audited exception to "SOLD never becomes FREE") and requests a full refund
 * via the gateway port. The refund settles asynchronously and never gates the cancellation.
 */
@Service
public class ReservationCancellationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationCancellationService.class);

    private final ReservationRepository reservations;
    private final ScreeningSeatRepository screeningSeats;
    private final TicketRepository tickets;
    private final ScreeningCatalog screenings;
    private final PaymentGateway paymentGateway;
    private final CancellationPublisher publisher;
    private final BookingProperties properties;

    ReservationCancellationService(
            ReservationRepository reservations,
            ScreeningSeatRepository screeningSeats,
            TicketRepository tickets,
            ScreeningCatalog screenings,
            PaymentGateway paymentGateway,
            CancellationPublisher publisher,
            BookingProperties properties) {
        this.reservations = reservations;
        this.screeningSeats = screeningSeats;
        this.tickets = tickets;
        this.screenings = screenings;
        this.paymentGateway = paymentGateway;
        this.publisher = publisher;
        this.properties = properties;
    }

    /** Cancels a reservation on the owner's request (SPEC-0018); owner only, state-machine guarded. */
    @Transactional
    public CancellationView cancel(UUID reservationId, UUID callerId) {
        var reservation = reservations.findById(reservationId).orElseThrow(ReservationNotFoundException::new);
        if (!reservation.userId().equals(callerId)) {
            throw new ReservationAccessDeniedException();
        }
        var previousStatus = reservation.status();
        var view =
                switch (previousStatus) {
                    case PENDING, AWAITING_PAYMENT -> cancelWithoutRefund(reservation, previousStatus);
                    case CONFIRMED -> cancelConfirmed(reservation, previousStatus);
                    case CANCELLED -> throw new AlreadyCancelledException();
                    case EXPIRED -> throw new ReservationExpiredException();
                };
        log.info("reservation cancelled reservationId={} previousStatus={}", reservationId, previousStatus);
        return view;
    }

    private CancellationView cancelWithoutRefund(Reservation reservation, ReservationStatus previousStatus) {
        var seatIds = releaseSeats(reservation, ScreeningSeat::release, ScreeningSeatStatus.HELD);
        reservation.cancel();
        publisher.publishCancelled(reservation, seatIds, previousStatus, false, 0);
        return new CancellationView(reservation.id(), reservation.status(), CancellationView.RefundInfo.none());
    }

    private CancellationView cancelConfirmed(Reservation reservation, ReservationStatus previousStatus) {
        var screening = screenings.find(reservation.screeningId()).orElseThrow(ReservationNotFoundException::new);
        var window = Duration.ofHours(properties.cancellationWindowHours());
        if (!cancellationWindowOpen(screening.startsAt(), Instant.now(), window)) {
            throw new CancellationWindowClosedException();
        }
        cancelTickets(reservation);
        var seatIds = releaseSeats(reservation, ScreeningSeat::releaseFromSold, ScreeningSeatStatus.SOLD);
        var refundCents = reservation.totalCents();
        paymentGateway.requestRefund(new RefundRequest(reservation.id(), refundCents));
        reservation.cancel();
        publisher.publishCancelled(reservation, seatIds, previousStatus, true, refundCents);
        return new CancellationView(
                reservation.id(), reservation.status(), CancellationView.RefundInfo.of(refundCents));
    }

    private void cancelTickets(Reservation reservation) {
        var reservationSeatIds =
                reservation.seats().stream().map(ReservationSeat::id).toList();
        tickets.findByReservationSeatIdIn(reservationSeatIds).forEach(Ticket::cancel);
    }

    private List<UUID> releaseSeats(
            Reservation reservation, Consumer<ScreeningSeat> transition, ScreeningSeatStatus expected) {
        var screeningSeatIds = reservation.seats().stream()
                .map(ReservationSeat::screeningSeatId)
                .toList();
        var seats = screeningSeats.findAllById(screeningSeatIds).stream()
                .filter(seat -> seat.status() == expected)
                .toList();
        seats.forEach(transition);
        return seats.stream().map(ScreeningSeat::seatId).toList();
    }

    /** The confirmed-cancellation window is open while at least {@code window} remains before start. */
    static boolean cancellationWindowOpen(Instant startsAt, Instant now, Duration window) {
        return Duration.between(now, startsAt).compareTo(window) >= 0;
    }
}
