package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The customer must verify their email before reserving (SPEC-0014). */
public class EmailNotVerifiedException extends BusinessException {

    public EmailNotVerifiedException() {
        super(HttpStatus.FORBIDDEN, "user.email-not-verified");
    }
}
