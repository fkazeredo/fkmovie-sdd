package com.fksoft.application.booking;

import com.fksoft.shared.error.DomainException;

/** Reservations are closed close to start time (SPEC-0014): {@code startsAt - now < 10 min}. */
public class ScreeningTooSoonException extends DomainException {

    public ScreeningTooSoonException() {
        super("booking.screening-too-soon");
    }
}
