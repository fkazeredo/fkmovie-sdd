package com.fksoft.domain.booking;

/**
 * Lifecycle of a reservation (SPEC-0014). v1 (this spec) only creates {@code PENDING} and holds
 * seats; {@code AWAITING_PAYMENT}/{@code CONFIRMED}/{@code CANCELLED}/{@code EXPIRED} transitions
 * are owned by payment/confirmation/expiration/cancellation (SPEC-0015–0018).
 */
public enum ReservationStatus {
    PENDING,
    AWAITING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    EXPIRED
}
