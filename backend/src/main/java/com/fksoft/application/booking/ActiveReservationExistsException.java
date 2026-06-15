package com.fksoft.application.booking;

import com.fksoft.shared.error.DomainException;

/** The user already holds an active reservation for this screening (SPEC-0014). */
public class ActiveReservationExistsException extends DomainException {

    public ActiveReservationExistsException() {
        super("booking.active-reservation-exists");
    }
}
