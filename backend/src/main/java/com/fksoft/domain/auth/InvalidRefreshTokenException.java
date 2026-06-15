package com.fksoft.domain.auth;

import com.fksoft.domain.error.DomainException;

/** Refresh token missing, unknown or expired (SPEC-0003). */
public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("auth.invalid-refresh");
    }
}
