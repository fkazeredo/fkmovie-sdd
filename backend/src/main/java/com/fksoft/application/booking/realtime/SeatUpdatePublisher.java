package com.fksoft.application.booking.realtime;

import com.fksoft.application.booking.SeatsStatusChanged;
import com.fksoft.application.cinema.CinemaCatalog;
import com.fksoft.application.cinema.SeatView;
import com.fksoft.application.screening.ScreeningCatalog;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes seat-status changes to the public topic (SPEC-0013, ADR 0009). Reacts AFTER_COMMIT to the
 * booking domain event and resolves each seat's row/number through the cinema facade — it only reads
 * and sends, never changing seat state. The realtime transport lives in {@code infra.realtime}; this
 * publisher uses the generic {@code SimpMessagingTemplate}.
 */
@Component
class SeatUpdatePublisher {

    private final SimpMessagingTemplate messaging;
    private final ScreeningCatalog screenings;
    private final CinemaCatalog cinema;
    private final MeterRegistry meterRegistry;

    SeatUpdatePublisher(
            SimpMessagingTemplate messaging,
            ScreeningCatalog screenings,
            CinemaCatalog cinema,
            MeterRegistry meterRegistry) {
        this.messaging = messaging;
        this.screenings = screenings;
        this.cinema = cinema;
        this.meterRegistry = meterRegistry;
    }

    @TransactionalEventListener
    void on(SeatsStatusChanged event) {
        var screening = screenings.find(event.screeningId()).orElse(null);
        if (screening == null) {
            return;
        }
        var viewsBySeatId = cinema.seatsOf(screening.roomId()).stream()
                .collect(Collectors.toMap(SeatView::seatId, Function.identity()));
        var seats = event.seatIds().stream()
                .map(viewsBySeatId::get)
                .filter(Objects::nonNull)
                .map(view -> new SeatUpdateMessage.Seat(view.seatId(), view.row(), view.number(), event.status()))
                .toList();
        messaging.convertAndSend(
                "/topic/screenings/" + event.screeningId() + "/seats",
                SeatUpdateMessage.of(event.screeningId(), seats));
        meterRegistry
                .counter("ws_messages_sent_total", "type", "SEAT_STATUS_CHANGED")
                .increment();
    }
}
