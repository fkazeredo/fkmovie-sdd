package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Change-password presented a wrong current password (SPEC-0003). */
public class PasswordMismatchException extends BusinessException {

    public PasswordMismatchException() {
        super(HttpStatus.UNAUTHORIZED, "auth.password-mismatch");
    }
}
