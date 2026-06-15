package com.fksoft.domain.auth;

import java.time.Instant;
import java.util.UUID;

/** An admin re-enabled a user (SPEC-0005); internal audit event. */
public record UserEnabled(UUID userId, UUID enabledByAdminId, Instant occurredAt) {}
