package com.fksoft.domain.booking;

import com.fksoft.domain.screening.ScreeningCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges screening domain events to booking inventory (SPEC-0009). Reacts AFTER_COMMIT so the
 * seats are only materialized once the screening is durable; the materialization itself runs in
 * its own transaction (idempotent).
 */
@Component
@RequiredArgsConstructor
class BookingScreeningListener {

    private final ScreeningSeatMaterializer materializer;

    @TransactionalEventListener
    void on(ScreeningCreated event) {
        materializer.materialize(event.screeningId(), event.roomId());
    }
}
