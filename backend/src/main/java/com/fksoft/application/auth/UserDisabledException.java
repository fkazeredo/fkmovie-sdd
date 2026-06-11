package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Credentials were correct, but the account is disabled (SPEC-0003: 403, never deleted). */
public class UserDisabledException extends BusinessException {

    public UserDisabledException() {
        super(HttpStatus.FORBIDDEN, "auth.disabled");
    }
}
