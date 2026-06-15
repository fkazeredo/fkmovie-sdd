package com.fksoft.domain.booking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Seats changed status for a screening (SPEC-0014). Published after commit; the realtime publisher
 * (SPEC-0013) turns it into a STOMP message on {@code /topic/screenings/{id}/seats}. No consumer
 * exists yet — the seat map is refreshed by REST until 0013 lands.
 */
public record SeatsStatusChanged(
        UUID screeningId, ScreeningSeatStatus status, List<UUID> seatIds, Instant occurredAt) {}
