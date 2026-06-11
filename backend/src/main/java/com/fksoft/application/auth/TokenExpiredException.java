package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** A verification/reset token that exists but is expired or already consumed (SPEC-0004: 410). */
public class TokenExpiredException extends BusinessException {

    public TokenExpiredException() {
        super(HttpStatus.GONE, "user.token-expired");
    }
}
