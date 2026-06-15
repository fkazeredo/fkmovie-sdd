package com.fksoft.domain.booking;

import com.fksoft.domain.auth.UserAccounts;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Emits the post-cancellation signals (SPEC-0018): the realtime FREE/CANCELLED events, the audit
 * {@link ReservationCancelled}, the {@link ReservationCancellationConfirmed} carrying the recipient +
 * refund outcome for the cancellation email, and the cancellation/refund metrics. The STOMP send is
 * the 0013 publishers'.
 */
@Component
class CancellationPublisher {

    private final UserAccounts userAccounts;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    CancellationPublisher(
            UserAccounts userAccounts, ApplicationEventPublisher events, MeterRegistry meterRegistry, Clock clock) {
        this.userAccounts = userAccounts;
        this.events = events;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    void publishCancelled(
            Reservation reservation,
            List<UUID> cinemaSeatIds,
            ReservationStatus previousStatus,
            boolean refundRequested,
            int refundCents) {
        var now = clock.instant();
        events.publishEvent(
                new SeatsStatusChanged(reservation.screeningId(), ScreeningSeatStatus.FREE, cinemaSeatIds, now));
        events.publishEvent(
                new ReservationStatusChanged(reservation.id(), reservation.userId(), ReservationStatus.CANCELLED, now));
        events.publishEvent(new ReservationCancelled(
                reservation.id(), reservation.userId(), CancellationReason.CUSTOMER, refundRequested, now));
        var account = userAccounts
                .find(reservation.userId())
                .orElseThrow(() -> new IllegalStateException("Account missing for reservation " + reservation.id()));
        events.publishEvent(new ReservationCancellationConfirmed(
                reservation.id(),
                reservation.userId(),
                account.email(),
                account.name(),
                account.preferredLocale(),
                refundRequested,
                refundCents,
                now));
        meterRegistry
                .counter("reservations_cancelled_total", "previousStatus", previousStatus.name())
                .increment();
        if (refundRequested) {
            meterRegistry.counter("refunds_requested_total").increment();
        }
    }
}
