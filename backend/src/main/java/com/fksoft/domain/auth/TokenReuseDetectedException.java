package com.fksoft.domain.auth;

import com.fksoft.domain.error.DomainException;

/**
 * A revoked refresh token was presented again — suspected token theft; every refresh token
 * of the user has been revoked as a precaution (SPEC-0003).
 */
public class TokenReuseDetectedException extends DomainException {

    public TokenReuseDetectedException() {
        super("auth.token-reuse-detected");
    }
}
