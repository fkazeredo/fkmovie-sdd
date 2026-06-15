package com.fksoft.application.realtime;

import com.fksoft.application.realtime.dto.ReservationStatusMessage;
import com.fksoft.domain.booking.ReservationStatusChanged;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes a reservation's status change to its owner's private queue (SPEC-0013, ADR 0009). Reacts
 * AFTER_COMMIT; Spring routes {@code /user/queue/...} to the principal bound at CONNECT, so only the
 * owner receives it.
 */
@Component
@RequiredArgsConstructor
class ReservationStatusPublisher {

    private final SimpMessagingTemplate messaging;
    private final MeterRegistry meterRegistry;

    @TransactionalEventListener
    void on(ReservationStatusChanged event) {
        messaging.convertAndSendToUser(
                event.userId().toString(),
                "/queue/reservations",
                ReservationStatusMessage.of(event.reservationId(), event.status()));
        meterRegistry
                .counter("ws_messages_sent_total", "type", "RESERVATION_STATUS_CHANGED")
                .increment();
    }
}
