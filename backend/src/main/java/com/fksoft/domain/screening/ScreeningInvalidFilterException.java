package com.fksoft.domain.screening;

import com.fksoft.domain.error.DomainException;

/** A public-list filter is malformed (SPEC-0010), e.g. a non-ISO date: 400. */
public class ScreeningInvalidFilterException extends DomainException {

    public ScreeningInvalidFilterException() {
        super("screening.invalid-filter");
    }
}
