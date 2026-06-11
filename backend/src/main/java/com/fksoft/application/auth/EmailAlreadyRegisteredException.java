package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Registration with an email that already exists (SPEC-0004). */
public class EmailAlreadyRegisteredException extends BusinessException {

    public EmailAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT, "user.email-taken");
    }
}
