package com.fksoft.application.screening;

import java.time.Instant;
import java.util.UUID;

/**
 * Stable screening projection exposed to other modules (SPEC-0011). Part of the screening
 * module's public read API ({@link ScreeningCatalog}); never the {@code Screening} entity.
 */
public record ScreeningView(
        UUID id,
        UUID movieId,
        UUID roomId,
        Instant startsAt,
        Instant endsAt,
        int basePriceCents,
        ScreeningStatus status) {}
