package com.fksoft.application.booking;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional unit of work behind the expiration sweep (SPEC-0017, ADR 0004). Claiming and each
 * reservation's expiry/cancellation run in their own transaction (called cross-bean by {@link
 * ReservationExpirationDispatcher} so the proxy applies them), so one poisoned row never blocks the
 * sweep. Held seats return to FREE and the realtime/audit events are published after commit; a row
 * already moved on (re-fetched and guarded) is a no-op, making repeated runs idempotent.
 */
@Component
class ReservationExpirationWorker {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationWorker.class);

    private final ReservationRepository reservations;
    private final ScreeningSeatRepository screeningSeats;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;
    private final BookingProperties properties;
    private final Clock clock;

    ReservationExpirationWorker(
            ReservationRepository reservations,
            ScreeningSeatRepository screeningSeats,
            ApplicationEventPublisher events,
            MeterRegistry meterRegistry,
            BookingProperties properties,
            Clock clock) {
        this.reservations = reservations;
        this.screeningSeats = screeningSeats;
        this.events = events;
        this.meterRegistry = meterRegistry;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    List<UUID> claimExpiredIds() {
        return reservations.claimExpired(clock.instant(), PageRequest.of(0, properties.expirationBatchSize())).stream()
                .map(Reservation::id)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    List<UUID> claimPaymentTimedOutIds() {
        return reservations
                .claimPaymentTimedOut(clock.instant(), PageRequest.of(0, properties.expirationBatchSize()))
                .stream()
                .map(Reservation::id)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void expire(UUID reservationId) {
        var reservation = reservations.findById(reservationId).orElse(null);
        if (reservation == null || !reservation.isPending()) {
            return; // confirmed/already expired under us — idempotent no-op
        }
        reservation.expire();
        var seatIds = releaseHeldSeats(reservation);
        publish(reservation, seatIds, ReservationStatus.EXPIRED);
        events.publishEvent(new ReservationExpired(reservation.id(), reservation.userId(), clock.instant()));
        meterRegistry.counter("reservations_expired_total").increment();
        log.info("reservation expired reservationId={} seats={}", reservationId, seatIds.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void cancelForPaymentTimeout(UUID reservationId) {
        var reservation = reservations.findById(reservationId).orElse(null);
        if (reservation == null || !reservation.isAwaitingPayment()) {
            return; // confirmed/cancelled under us — idempotent no-op
        }
        reservation.cancelForPaymentTimeout();
        var seatIds = releaseHeldSeats(reservation);
        publish(reservation, seatIds, ReservationStatus.CANCELLED);
        events.publishEvent(new ReservationCancelled(
                reservation.id(), reservation.userId(), CancellationReason.PAYMENT_TIMEOUT, clock.instant()));
        meterRegistry.counter("reservations_payment_timeout_total").increment();
        log.info("reservation payment timed out reservationId={} seats={}", reservationId, seatIds.size());
    }

    private List<UUID> releaseHeldSeats(Reservation reservation) {
        var screeningSeatIds = reservation.seats().stream()
                .map(ReservationSeat::screeningSeatId)
                .toList();
        var held = screeningSeats.findAllById(screeningSeatIds).stream()
                .filter(seat -> seat.status() == ScreeningSeatStatus.HELD)
                .toList();
        held.forEach(ScreeningSeat::release);
        return held.stream().map(ScreeningSeat::seatId).toList();
    }

    private void publish(Reservation reservation, List<UUID> cinemaSeatIds, ReservationStatus status) {
        var now = clock.instant();
        events.publishEvent(
                new SeatsStatusChanged(reservation.screeningId(), ScreeningSeatStatus.FREE, cinemaSeatIds, now));
        events.publishEvent(new ReservationStatusChanged(reservation.id(), reservation.userId(), status, now));
    }
}
