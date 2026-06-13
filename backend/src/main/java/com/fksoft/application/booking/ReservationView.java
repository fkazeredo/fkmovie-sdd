package com.fksoft.application.booking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reservation read model (SPEC-0014/0016/0019): assembled by the service from the reservation plus the
 * screening/movie/room display info, the cinema seat details and (once confirmed) the issued tickets,
 * and the refund summary once cancelled-with-refund. Lives in the module root (built by the service,
 * joins cinema/screening/payment), not the {@code api} package — same rule as the seat map.
 */
public record ReservationView(
        UUID reservationId,
        UUID screeningId,
        ReservationStatus status,
        String movieTitle,
        String roomName,
        Instant startsAt,
        Instant expiresAt,
        Instant paymentDeadlineAt,
        int totalCents,
        List<ReservationSeatView> seats,
        List<TicketView> tickets,
        RefundSummary refund) {

    /** One seat of the reservation, with display info from the cinema and the snapshotted price. */
    public record ReservationSeatView(
            UUID seatId,
            String row,
            int number,
            com.fksoft.application.cinema.SeatType type,
            com.fksoft.application.pricing.TicketType ticketType,
            int priceCents) {}

    /** One issued ticket (SPEC-0016): its code, seat label and status. Empty until CONFIRMED. */
    public record TicketView(UUID ticketId, String code, String seatLabel, TicketStatus status) {}

    /** Refund summary (SPEC-0019): present only when a refund was requested (cancelled CONFIRMED). */
    public record RefundSummary(int amountCents, String status) {}
}
