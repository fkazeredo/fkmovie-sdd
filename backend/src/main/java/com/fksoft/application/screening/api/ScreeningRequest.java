package com.fksoft.application.screening.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

/**
 * Create/update payload for a screening (SPEC-0009). The 1-hour-future and room-overlap rules are
 * business checks in the service; {@code @Future} only rejects clearly past timestamps here.
 */
public record ScreeningRequest(
        @NotNull UUID movieId,
        @NotNull UUID roomId,
        @NotNull @Future Instant startsAt,
        @NotNull @Positive Integer basePriceCents) {}
