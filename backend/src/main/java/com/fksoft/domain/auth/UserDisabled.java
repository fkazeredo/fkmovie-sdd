package com.fksoft.domain.auth;

import java.time.Instant;
import java.util.UUID;

/** An admin disabled a user (SPEC-0005); internal audit event. */
public record UserDisabled(UUID userId, UUID disabledByAdminId, Instant occurredAt) {}
