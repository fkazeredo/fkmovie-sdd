package com.fksoft.application.booking;

import com.fksoft.shared.error.DomainException;

/** The operator search did not provide exactly one criterion (SPEC-0020): 400. */
public class OperatorInvalidSearchException extends DomainException {

    public OperatorInvalidSearchException() {
        super("operator.invalid-search");
    }
}
