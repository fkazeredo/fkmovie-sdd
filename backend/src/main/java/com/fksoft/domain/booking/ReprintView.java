package com.fksoft.domain.booking;

import java.time.Instant;

/**
 * Printable payload for an operator ticket reprint (SPEC-0020): everything the print-friendly page
 * (0026) needs, plus the reprint count for fraud awareness. The ticket state is unchanged.
 */
public record ReprintView(
        String ticketCode,
        String seatLabel,
        String movieTitle,
        String roomName,
        Instant startsAt,
        String customerName,
        long reprintCount) {}
