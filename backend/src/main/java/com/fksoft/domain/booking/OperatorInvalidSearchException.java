package com.fksoft.domain.booking;

import com.fksoft.domain.error.DomainException;

/** The operator search did not provide exactly one criterion (SPEC-0020): 400. */
public class OperatorInvalidSearchException extends DomainException {

    public OperatorInvalidSearchException() {
        super("operator.invalid-search");
    }
}
