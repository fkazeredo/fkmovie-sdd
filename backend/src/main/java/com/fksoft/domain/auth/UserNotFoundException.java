package com.fksoft.domain.auth;

import com.fksoft.domain.error.DomainException;

/** Admin referenced an unknown user id (SPEC-0005). */
public class UserNotFoundException extends DomainException {

    public UserNotFoundException() {
        super("user.not-found");
    }
}
