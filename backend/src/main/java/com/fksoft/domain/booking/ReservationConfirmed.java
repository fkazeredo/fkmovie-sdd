package com.fksoft.domain.booking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A reservation was confirmed and its tickets issued (SPEC-0016). Carries the recipient's contact
 * (so the notification module needs no auth lookup) and the issued tickets for the email.
 */
public record ReservationConfirmed(
        UUID reservationId,
        UUID userId,
        String email,
        String name,
        String preferredLocale,
        List<TicketInfo> tickets,
        Instant occurredAt) {

    /** A ticket on the confirmation email: its code and seat label (e.g. {@code A1}). */
    public record TicketInfo(String code, String seatLabel) {}
}
