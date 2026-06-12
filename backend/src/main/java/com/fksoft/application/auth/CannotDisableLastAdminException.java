package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Disabling would leave the system with no active admin (SPEC-0005). */
public class CannotDisableLastAdminException extends BusinessException {

    public CannotDisableLastAdminException() {
        super(HttpStatus.CONFLICT, "user.cannot-disable-last-admin");
    }
}
