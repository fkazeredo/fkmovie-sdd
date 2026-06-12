package com.fksoft.application.screening;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Lookup of a screening that does not exist (SPEC-0009). */
public class ScreeningNotFoundException extends BusinessException {

    public ScreeningNotFoundException() {
        super(HttpStatus.NOT_FOUND, "screening.not-found");
    }
}
