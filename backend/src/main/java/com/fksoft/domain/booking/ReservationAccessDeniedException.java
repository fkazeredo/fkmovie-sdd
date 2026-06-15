package com.fksoft.domain.booking;

import com.fksoft.domain.error.DomainException;

/** The caller is neither the reservation owner nor staff (SPEC-0014/0019): 403, business ownership. */
public class ReservationAccessDeniedException extends DomainException {

    public ReservationAccessDeniedException() {
        super("booking.not-owner");
    }
}
