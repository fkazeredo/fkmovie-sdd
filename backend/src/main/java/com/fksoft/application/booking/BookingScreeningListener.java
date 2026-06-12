package com.fksoft.application.booking;

import com.fksoft.application.screening.ScreeningCreated;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges screening domain events to booking inventory (SPEC-0009). Reacts AFTER_COMMIT so the
 * seats are only materialized once the screening is durable; the materialization itself runs in
 * its own transaction (idempotent).
 */
@Component
class BookingScreeningListener {

    private final ScreeningSeatMaterializer materializer;

    BookingScreeningListener(ScreeningSeatMaterializer materializer) {
        this.materializer = materializer;
    }

    @TransactionalEventListener
    void on(ScreeningCreated event) {
        materializer.materialize(event.screeningId(), event.roomId());
    }
}
