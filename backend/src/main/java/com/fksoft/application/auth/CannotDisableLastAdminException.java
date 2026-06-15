package com.fksoft.application.auth;

import com.fksoft.shared.error.DomainException;

/** Disabling would leave the system with no active admin (SPEC-0005). */
public class CannotDisableLastAdminException extends DomainException {

    public CannotDisableLastAdminException() {
        super("user.cannot-disable-last-admin");
    }
}
