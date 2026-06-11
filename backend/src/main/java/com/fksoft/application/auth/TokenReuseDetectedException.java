package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * A revoked refresh token was presented again — suspected token theft; every refresh token
 * of the user has been revoked as a precaution (SPEC-0003).
 */
public class TokenReuseDetectedException extends BusinessException {

    public TokenReuseDetectedException() {
        super(HttpStatus.UNAUTHORIZED, "auth.token-reuse-detected");
    }
}
