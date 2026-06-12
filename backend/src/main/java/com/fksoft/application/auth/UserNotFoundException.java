package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Admin referenced an unknown user id (SPEC-0005). */
public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND, "user.not-found");
    }
}
