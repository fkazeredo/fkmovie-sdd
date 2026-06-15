package com.fksoft.domain.booking;

import com.fksoft.domain.error.DomainException;

/** The reservation is already confirmed (SPEC-0016). */
public class AlreadyConfirmedException extends DomainException {

    public AlreadyConfirmedException() {
        super("booking.already-confirmed");
    }
}
