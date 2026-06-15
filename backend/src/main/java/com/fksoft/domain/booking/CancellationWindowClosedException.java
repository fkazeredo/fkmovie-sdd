package com.fksoft.domain.booking;

import com.fksoft.domain.error.DomainException;

/** A confirmed reservation is too close to the session start to self-cancel (SPEC-0018): 409. */
public class CancellationWindowClosedException extends DomainException {

    public CancellationWindowClosedException() {
        super("booking.cancellation-window-closed");
    }
}
