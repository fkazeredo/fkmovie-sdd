package com.fksoft.application.booking;

import com.fksoft.application.cinema.CinemaCatalog;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Materializes the seat inventory of a screening (SPEC-0009): one {@code FREE} {@link
 * ScreeningSeat} per physical seat of the room, read through the cinema module's public
 * {@link CinemaCatalog}. Idempotent — skips if already materialized, and the
 * {@code UNIQUE(screening_id, seat_id)} constraint makes any race safe.
 */
@Service
class ScreeningSeatMaterializer {

    private static final Logger log = LoggerFactory.getLogger(ScreeningSeatMaterializer.class);

    private final ScreeningSeatRepository screeningSeats;
    private final CinemaCatalog cinema;

    ScreeningSeatMaterializer(ScreeningSeatRepository screeningSeats, CinemaCatalog cinema) {
        this.screeningSeats = screeningSeats;
        this.cinema = cinema;
    }

    /**
     * Creates the FREE inventory rows. REQUIRES_NEW because the only caller is an AFTER_COMMIT
     * listener — a REQUIRED transaction would join the already-committed source transaction and
     * never flush.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void materialize(UUID screeningId, UUID roomId) {
        if (screeningSeats.existsByScreeningId(screeningId)) {
            return;
        }
        var rows = cinema.seatsOf(roomId).stream()
                .map(seat -> ScreeningSeat.free(screeningId, seat.seatId()))
                .toList();
        screeningSeats.saveAll(rows);
        log.info("materialized screening inventory screeningId={} seats={}", screeningId, rows.size());
    }
}
