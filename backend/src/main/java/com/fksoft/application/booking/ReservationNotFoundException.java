package com.fksoft.application.booking;

import com.fksoft.shared.error.DomainException;

/** Lookup of a reservation that does not exist (SPEC-0014). */
public class ReservationNotFoundException extends DomainException {

    public ReservationNotFoundException() {
        super("booking.reservation-not-found");
    }
}
