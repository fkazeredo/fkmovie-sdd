package com.fksoft.domain.booking;

/** Lifecycle of a ticket (SPEC-0016): issued VALID; CANCELLED only via cancellation (0018). */
public enum TicketStatus {
    VALID,
    CANCELLED
}
