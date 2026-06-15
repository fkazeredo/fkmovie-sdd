package com.fksoft.application.booking;

import com.fksoft.shared.error.DomainException;

/** The reservation hold has expired and cannot be confirmed (SPEC-0016): 410 Gone. */
public class ReservationExpiredException extends DomainException {

    public ReservationExpiredException() {
        super("booking.reservation-expired");
    }
}
