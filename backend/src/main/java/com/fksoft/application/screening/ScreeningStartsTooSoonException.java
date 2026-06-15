package com.fksoft.application.screening;

import com.fksoft.shared.error.DomainException;

/** The screening starts less than one hour in the future (SPEC-0009). */
public class ScreeningStartsTooSoonException extends DomainException {

    public ScreeningStartsTooSoonException() {
        super("screening.starts-too-soon");
    }
}
