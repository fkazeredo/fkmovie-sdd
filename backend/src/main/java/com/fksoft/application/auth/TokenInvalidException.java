package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** A verification/reset token whose value is unknown or malformed (SPEC-0004: 400). */
public class TokenInvalidException extends BusinessException {

    public TokenInvalidException() {
        super(HttpStatus.BAD_REQUEST, "user.token-invalid");
    }
}
