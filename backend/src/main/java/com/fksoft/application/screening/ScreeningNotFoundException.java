package com.fksoft.application.screening;

import com.fksoft.shared.error.DomainException;

/** Lookup of a screening that does not exist (SPEC-0009). */
public class ScreeningNotFoundException extends DomainException {

    public ScreeningNotFoundException() {
        super("screening.not-found");
    }
}
