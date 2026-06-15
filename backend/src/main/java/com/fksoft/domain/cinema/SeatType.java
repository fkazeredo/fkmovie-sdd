package com.fksoft.domain.cinema;

/**
 * Seat category following standard Brazilian cinema layout (SPEC-0007). Drives pricing
 * (SPEC-0012) and seat-map rendering (SPEC-0023).
 */
public enum SeatType {
    STANDARD,
    VIP,
    ACCESSIBLE,
    COMPANION
}
