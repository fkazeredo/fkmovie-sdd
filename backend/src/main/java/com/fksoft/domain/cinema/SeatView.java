package com.fksoft.domain.cinema;

import java.util.UUID;

/**
 * Stable seat projection exposed to other modules (SPEC-0009). Part of the cinema module's
 * public read API ({@link CinemaCatalog}); never the {@code Seat} entity itself.
 */
public record SeatView(UUID seatId, String row, int number, SeatType type) {}
