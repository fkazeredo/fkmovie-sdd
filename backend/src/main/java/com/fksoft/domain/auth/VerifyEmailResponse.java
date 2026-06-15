package com.fksoft.domain.auth;

/** Response of POST /api/users/verify-email (SPEC-0004). */
public record VerifyEmailResponse(String email, boolean emailVerified) {

    static VerifyEmailResponse from(User user) {
        return new VerifyEmailResponse(user.email(), user.isEmailVerified());
    }
}
