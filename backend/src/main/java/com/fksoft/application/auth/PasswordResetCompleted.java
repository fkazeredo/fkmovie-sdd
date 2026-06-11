package com.fksoft.application.auth;

import java.time.Instant;
import java.util.UUID;

/** A password reset completed (SPEC-0004); triggers refresh-token revocation auditing. */
public record PasswordResetCompleted(UUID userId, Instant occurredAt) {}
