package com.fksoft.domain.booking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One reservation in an operator search result (SPEC-0020): identity plus screening summary. The
 * email is partially masked here (full email is only on the detail view). Module-root DTO.
 */
public record OperatorReservationView(
        UUID reservationId,
        ReservationStatus status,
        String customerName,
        String customerEmail,
        String movieTitle,
        Instant startsAt,
        List<String> seatLabels) {}
