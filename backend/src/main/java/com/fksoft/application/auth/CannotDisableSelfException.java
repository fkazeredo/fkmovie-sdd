package com.fksoft.application.auth;

import com.fksoft.shared.error.DomainException;

/** An admin tried to disable their own account (SPEC-0005). */
public class CannotDisableSelfException extends DomainException {

    public CannotDisableSelfException() {
        super("user.cannot-disable-self");
    }
}
