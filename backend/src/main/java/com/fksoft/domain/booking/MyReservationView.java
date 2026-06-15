package com.fksoft.domain.booking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One row of the customer's reservation history (SPEC-0019): the reservation plus its screening/movie
 * display info and seat labels. Lives in the module root (assembled by the service from cinema/
 * screening reads), not the {@code api} package — same rule as the seat map.
 */
public record MyReservationView(
        UUID reservationId,
        ReservationStatus status,
        String movieTitle,
        String roomName,
        Instant startsAt,
        int totalCents,
        List<String> seatLabels) {}
