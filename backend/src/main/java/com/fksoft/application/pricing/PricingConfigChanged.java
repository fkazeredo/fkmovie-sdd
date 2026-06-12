package com.fksoft.application.pricing;

import java.time.Instant;
import java.util.UUID;

/** A pricing modifier was changed (SPEC-0012). Triggers the in-memory snapshot reload; audit. */
public record PricingConfigChanged(UUID changedByAdminId, Instant occurredAt) {}
