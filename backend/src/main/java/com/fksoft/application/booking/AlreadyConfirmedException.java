package com.fksoft.application.booking;

import com.fksoft.shared.error.DomainException;

/** The reservation is already confirmed (SPEC-0016). */
public class AlreadyConfirmedException extends DomainException {

    public AlreadyConfirmedException() {
        super("booking.already-confirmed");
    }
}
