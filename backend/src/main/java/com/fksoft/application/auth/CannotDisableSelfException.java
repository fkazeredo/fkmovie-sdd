package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** An admin tried to disable their own account (SPEC-0005). */
public class CannotDisableSelfException extends BusinessException {

    public CannotDisableSelfException() {
        super(HttpStatus.CONFLICT, "user.cannot-disable-self");
    }
}
