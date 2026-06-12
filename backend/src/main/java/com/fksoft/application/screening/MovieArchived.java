package com.fksoft.application.screening;

import java.time.Instant;
import java.util.UUID;

/**
 * A movie was archived (SPEC-0008). Public module event: its consumer — the public-list cache
 * invalidation hook — arrives with SPEC-0010, so it has no listener yet.
 */
public record MovieArchived(UUID movieId, Instant occurredAt) {}
