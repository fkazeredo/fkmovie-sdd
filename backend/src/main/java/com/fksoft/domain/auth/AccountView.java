package com.fksoft.domain.auth;

import java.util.UUID;

/**
 * Stable account projection exposed to other modules (SPEC-0014). Part of the auth module's public
 * read API ({@link UserAccounts}); never the {@code User} entity. Lets the booking module check the
 * caller's live role and email-verification without touching auth persistence.
 */
public record AccountView(
        UUID userId,
        String email,
        String name,
        String preferredLocale,
        Role role,
        UserStatus status,
        boolean emailVerified) {}
