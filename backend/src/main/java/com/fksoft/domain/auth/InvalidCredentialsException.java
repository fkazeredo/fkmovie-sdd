package com.fksoft.domain.auth;

import com.fksoft.domain.error.DomainException;

/** Wrong email or password; deliberately indistinguishable to avoid user enumeration. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("auth.invalid-credentials");
    }
}
