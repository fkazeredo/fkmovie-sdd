package com.fksoft.application.auth;

import com.fksoft.shared.error.DomainException;

/** Registration with an email that already exists (SPEC-0004). */
public class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException() {
        super("user.email-taken");
    }
}
