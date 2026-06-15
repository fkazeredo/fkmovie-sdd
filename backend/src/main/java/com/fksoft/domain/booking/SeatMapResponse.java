package com.fksoft.domain.booking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public seat map of a screening (SPEC-0011): the module's read model, assembled by
 * {@link SeatMapService}. Stable contract — never exposes entities.
 */
public record SeatMapResponse(UUID screeningId, String roomName, Instant startsAt, List<SeatMapSeat> seats) {}
