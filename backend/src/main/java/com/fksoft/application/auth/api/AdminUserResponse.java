package com.fksoft.application.auth.api;

import com.fksoft.application.auth.Role;
import com.fksoft.application.auth.User;
import com.fksoft.application.auth.UserStatus;
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
