package com.fksoft.application.auth;

import com.fksoft.shared.error.DomainException;

/** Wrong email or password; deliberately indistinguishable to avoid user enumeration. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("auth.invalid-credentials");
    }
}
