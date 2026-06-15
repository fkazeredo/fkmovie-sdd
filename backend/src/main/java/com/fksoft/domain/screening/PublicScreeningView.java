package com.fksoft.domain.screening;

import java.time.Instant;
import java.util.UUID;

/**
 * One upcoming session on the public listing (SPEC-0010): movie/room display info plus the "from"
 * price (cheapest possible full price). A stable DTO — never the {@code Screening}/{@code Movie} entity.
 */
public record PublicScreeningView(
        UUID id,
        String movieTitle,
        AgeRating ageRating,
        String posterUrl,
        String roomName,
        Instant startsAt,
        int durationMinutes,
        int fromPriceCents) {}
