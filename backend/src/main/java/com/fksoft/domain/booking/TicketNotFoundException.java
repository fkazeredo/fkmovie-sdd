package com.fksoft.domain.booking;

import com.fksoft.domain.error.DomainException;

/** Lookup/reprint of a ticket that does not exist (SPEC-0020): 404. */
public class TicketNotFoundException extends DomainException {

    public TicketNotFoundException() {
        super("booking.ticket-not-found");
    }
}
