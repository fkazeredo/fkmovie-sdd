package com.fksoft.domain.booking;

import java.time.Instant;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Generates ticket codes {@code FKM-YYYY-NNNNNN} (SPEC-0016): the year is informational, the number
 * comes from a single global Postgres sequence (unique, concurrency-safe, no annual reset).
 */
@Component
@RequiredArgsConstructor
class TicketCodeGenerator {

    private final TicketRepository tickets;

    String next(Instant issuedAt) {
        var year = issuedAt.atZone(ZoneOffset.UTC).getYear();
        return "FKM-%d-%06d".formatted(year, tickets.nextCodeValue());
    }
}
