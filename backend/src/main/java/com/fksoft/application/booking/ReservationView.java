package com.fksoft.application.booking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reservation read model (SPEC-0014): assembled by the service from the reservation plus the cinema
 * seat details. Lives in the module root (built by the service, joins cinema/screening), not the
 * {@code api} package — same rule as the seat map.
 */
public record ReservationView(
        UUID reservationId,
        UUID screeningId,
        ReservationStatus status,
        Instant expiresAt,
        int totalCents,
        List<ReservationSeatView> seats) {

    /** One seat of the reservation, with display info from the cinema and the snapshotted price. */
    public record ReservationSeatView(
            UUID seatId,
            String row,
            int number,
            com.fksoft.application.cinema.SeatType type,
            com.fksoft.application.pricing.TicketType ticketType,
            int priceCents) {}
}
