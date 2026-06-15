package com.fksoft.application.auth;

import com.fksoft.shared.error.DomainException;

/** A verification/reset token that exists but is expired or already consumed (SPEC-0004: 410). */
public class TokenExpiredException extends DomainException {

    public TokenExpiredException() {
        super("user.token-expired");
    }
}
