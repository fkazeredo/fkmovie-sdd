package com.fksoft.application.auth;

import com.fksoft.shared.error.DomainException;

/** Change-password presented a wrong current password (SPEC-0003). */
public class PasswordMismatchException extends DomainException {

    public PasswordMismatchException() {
        super("auth.password-mismatch");
    }
}
