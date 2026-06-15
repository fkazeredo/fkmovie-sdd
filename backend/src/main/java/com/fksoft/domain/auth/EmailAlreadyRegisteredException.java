package com.fksoft.domain.auth;

import com.fksoft.domain.error.DomainException;

/** Registration with an email that already exists (SPEC-0004). */
public class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException() {
        super("user.email-taken");
    }
}
