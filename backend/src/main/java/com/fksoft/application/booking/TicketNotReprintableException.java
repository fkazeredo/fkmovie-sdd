package com.fksoft.application.booking;

import com.fksoft.shared.error.DomainException;

/** The ticket cannot be reprinted — not VALID, or its reservation is not CONFIRMED (SPEC-0020): 409. */
public class TicketNotReprintableException extends DomainException {

    public TicketNotReprintableException() {
        super("booking.ticket-not-reprintable");
    }
}
