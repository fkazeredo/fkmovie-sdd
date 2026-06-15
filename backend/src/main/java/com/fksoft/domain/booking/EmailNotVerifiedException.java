package com.fksoft.domain.booking;

import com.fksoft.domain.error.DomainException;

/** The customer must verify their email before reserving (SPEC-0014). */
public class EmailNotVerifiedException extends DomainException {

    public EmailNotVerifiedException() {
        super("user.email-not-verified");
    }
}
