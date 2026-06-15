package com.fksoft.domain.auth;

import java.time.Instant;
import java.util.UUID;

/** An admin changed a user's role (SPEC-0005); internal audit event. */
public record UserRoleChanged(UUID userId, Role oldRole, Role newRole, UUID changedByAdminId, Instant occurredAt) {}
