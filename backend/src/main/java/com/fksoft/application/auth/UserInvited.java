package com.fksoft.application.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * An admin invited an internal user (SPEC-0005); triggers the invitation email. The raw
 * token rides on the event so the notification listener can build the accept link (the event
 * is in-process, AFTER_COMMIT, never externalized — same approach as {@link CustomerRegistered}).
 */
public record UserInvited(
        UUID userId,
        String email,
        String name,
        String preferredLocale,
        Role role,
        String invitationToken,
        UUID invitedByAdminId,
        Instant occurredAt) {}
