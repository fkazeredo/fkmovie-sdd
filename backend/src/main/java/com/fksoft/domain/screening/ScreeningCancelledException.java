package com.fksoft.domain.screening;

import com.fksoft.domain.error.DomainException;

/** The screening was cancelled — its seat map is gone (SPEC-0011): HTTP 410 Gone. */
public class ScreeningCancelledException extends DomainException {

    public ScreeningCancelledException() {
        super("screening.cancelled");
    }
}
