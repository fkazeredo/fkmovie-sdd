package com.fksoft.application.auth;

import com.fksoft.shared.error.DomainException;

/** Refresh token missing, unknown or expired (SPEC-0003). */
public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("auth.invalid-refresh");
    }
}
