package com.fksoft.application.auth;

import java.time.Instant;
import java.util.UUID;

/** Business fact for audit/observability (SPEC-0003); internal event, not exposed in v1. */
public record UserLoggedOut(UUID userId, Instant occurredAt) {}
