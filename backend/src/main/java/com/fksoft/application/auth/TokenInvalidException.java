package com.fksoft.application.auth;

import com.fksoft.shared.error.DomainException;

/** A verification/reset token whose value is unknown or malformed (SPEC-0004: 400). */
public class TokenInvalidException extends DomainException {

    public TokenInvalidException() {
        super("user.token-invalid");
    }
}
