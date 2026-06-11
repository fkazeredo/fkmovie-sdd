package com.fksoft.application.auth.api;

import com.fksoft.application.auth.Role;
import com.fksoft.application.auth.User;
import java.util.UUID;

public record UserSummaryResponse(UUID id, String email, String name, Role role, boolean emailVerified) {

    static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.id(), user.email(), user.name(), user.role(), user.isEmailVerified());
    }
}
