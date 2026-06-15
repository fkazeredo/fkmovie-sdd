package com.fksoft.domain.booking;

import com.fksoft.domain.error.DomainException;

/** Reservations are closed close to start time (SPEC-0014): {@code startsAt - now < 10 min}. */
public class ScreeningTooSoonException extends DomainException {

    public ScreeningTooSoonException() {
        super("booking.screening-too-soon");
    }
}
