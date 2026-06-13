package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Lookup/reprint of a ticket that does not exist (SPEC-0020): 404. */
public class TicketNotFoundException extends BusinessException {

    public TicketNotFoundException() {
        super(HttpStatus.NOT_FOUND, "booking.ticket-not-found");
    }
}
