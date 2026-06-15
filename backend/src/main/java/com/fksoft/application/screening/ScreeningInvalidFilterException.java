package com.fksoft.application.screening;

import com.fksoft.shared.error.DomainException;

/** A public-list filter is malformed (SPEC-0010), e.g. a non-ISO date: 400. */
public class ScreeningInvalidFilterException extends DomainException {

    public ScreeningInvalidFilterException() {
        super("screening.invalid-filter");
    }
}
