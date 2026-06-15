package com.fksoft.domain.booking;

import com.fksoft.domain.error.DomainException;

/** The reservation was cancelled and cannot be confirmed (SPEC-0016). */
public class ReservationCancelledException extends DomainException {

    public ReservationCancelledException() {
        super("booking.reservation-cancelled");
    }
}
