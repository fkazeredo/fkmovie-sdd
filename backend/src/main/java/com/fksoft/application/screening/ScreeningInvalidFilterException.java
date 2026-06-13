package com.fksoft.application.screening;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** A public-list filter is malformed (SPEC-0010), e.g. a non-ISO date: 400. */
public class ScreeningInvalidFilterException extends BusinessException {

    public ScreeningInvalidFilterException() {
        super(HttpStatus.BAD_REQUEST, "screening.invalid-filter");
    }
}
