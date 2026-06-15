package com.fksoft.domain.auth;

import com.fksoft.domain.error.DomainException;

/** Change-password presented a wrong current password (SPEC-0003). */
public class PasswordMismatchException extends DomainException {

    public PasswordMismatchException() {
        super("auth.password-mismatch");
    }
}
