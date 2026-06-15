package com.fksoft.application.auth;

import com.fksoft.shared.error.DomainException;

/** Admin referenced an unknown user id (SPEC-0005). */
public class UserNotFoundException extends DomainException {

    public UserNotFoundException() {
        super("user.not-found");
    }
}
