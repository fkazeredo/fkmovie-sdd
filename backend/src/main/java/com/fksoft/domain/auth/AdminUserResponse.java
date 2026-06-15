package com.fksoft.domain.auth;

import java.time.Instant;
import java.util.UUID;

/** Admin-facing user view (SPEC-0005). */
public record AdminUserResponse(
        UUID id, String email, String name, Role role, UserStatus status, boolean emailVerified, Instant invitedAt) {

    /** Maps a user entity to its admin-facing view. */
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.id(),
                user.email(),
                user.name(),
                user.role(),
                user.status(),
                user.isEmailVerified(),
                user.invitedAt());
    }
}
