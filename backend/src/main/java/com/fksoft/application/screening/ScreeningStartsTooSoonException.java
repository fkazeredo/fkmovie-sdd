package com.fksoft.application.screening;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The screening starts less than one hour in the future (SPEC-0009). */
public class ScreeningStartsTooSoonException extends BusinessException {

    public ScreeningStartsTooSoonException() {
        super(HttpStatus.BAD_REQUEST, "screening.starts-too-soon");
    }
}
