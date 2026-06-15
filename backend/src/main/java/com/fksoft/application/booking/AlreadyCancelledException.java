package com.fksoft.application.booking;

import com.fksoft.shared.error.DomainException;

/** The reservation is already CANCELLED — nothing to cancel (SPEC-0018): 409. */
public class AlreadyCancelledException extends DomainException {

    public AlreadyCancelledException() {
        super("booking.already-cancelled");
    }
}
