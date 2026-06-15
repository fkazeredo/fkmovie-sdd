package com.fksoft.domain.screening;

import com.fksoft.domain.error.DomainException;

/** Lookup of a screening that does not exist (SPEC-0009). */
public class ScreeningNotFoundException extends DomainException {

    public ScreeningNotFoundException() {
        super("screening.not-found");
    }
}
