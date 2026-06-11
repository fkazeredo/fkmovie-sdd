package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Refresh token missing, unknown or expired (SPEC-0003). */
public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, "auth.invalid-refresh");
    }
}
