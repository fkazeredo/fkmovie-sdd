package com.fksoft.application.auth.api;

import com.fksoft.application.auth.User;

/** Response of POST /api/users/verify-email (SPEC-0004). */
public record VerifyEmailResponse(String email, boolean emailVerified) {

    static VerifyEmailResponse from(User user) {
        return new VerifyEmailResponse(user.email(), user.isEmailVerified());
    }
}
