package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The operator search did not provide exactly one criterion (SPEC-0020): 400. */
public class OperatorInvalidSearchException extends BusinessException {

    public OperatorInvalidSearchException() {
        super(HttpStatus.BAD_REQUEST, "operator.invalid-search");
    }
}
