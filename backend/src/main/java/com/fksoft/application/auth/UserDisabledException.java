package com.fksoft.application.auth;

import com.fksoft.shared.error.DomainException;

/** Credentials were correct, but the account is disabled (SPEC-0003: 403, never deleted). */
public class UserDisabledException extends DomainException {

    public UserDisabledException() {
        super("auth.disabled");
    }
}
