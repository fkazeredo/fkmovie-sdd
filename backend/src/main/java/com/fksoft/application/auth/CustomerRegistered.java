package com.fksoft.application.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer self-registered (SPEC-0004); triggers the verification email. The raw token
 * rides on the event so the notification listener can build the link without re-reading the
 * DB — the event is in-process (AFTER_COMMIT) and never externalized.
 */
public record CustomerRegistered(
        UUID userId, String email, String name, String preferredLocale, String verificationToken, Instant occurredAt) {}
