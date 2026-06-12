package com.fksoft.application.screening;

import java.time.Instant;
import java.util.UUID;

/**
 * A screening was scheduled (SPEC-0009). Public module event consumed by the booking module to
 * materialize the screening's seat inventory (after commit, idempotent).
 */
public record ScreeningCreated(UUID screeningId, UUID roomId, Instant occurredAt) {}
