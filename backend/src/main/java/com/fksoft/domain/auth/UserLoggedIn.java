package com.fksoft.domain.auth;

import java.time.Instant;
import java.util.UUID;

/** Business fact for audit/observability (SPEC-0003); internal event, not exposed in v1. */
public record UserLoggedIn(UUID userId, String ip, String userAgent, Instant occurredAt) {}
