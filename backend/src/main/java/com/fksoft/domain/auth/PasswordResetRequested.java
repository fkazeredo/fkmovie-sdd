package com.fksoft.domain.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * A password reset was requested (SPEC-0004); triggers the reset email. The raw token rides
 * on the event for link building (see {@link CustomerRegistered}).
 */
public record PasswordResetRequested(
        UUID userId, String email, String name, String preferredLocale, String resetToken, Instant occurredAt) {}
