package com.fksoft.domain.auth;

import com.fksoft.domain.error.DomainException;

/** Credentials were correct, but the account is disabled (SPEC-0003: 403, never deleted). */
public class UserDisabledException extends DomainException {

    public UserDisabledException() {
        super("auth.disabled");
    }
}
