package com.fksoft.domain.auth;

import java.time.Instant;
import java.util.UUID;

/** A customer's email was verified (SPEC-0004); internal event for audit. */
public record EmailVerified(UUID userId, Instant occurredAt) {}
