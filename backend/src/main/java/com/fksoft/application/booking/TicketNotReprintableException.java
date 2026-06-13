package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The ticket cannot be reprinted — not VALID, or its reservation is not CONFIRMED (SPEC-0020): 409. */
public class TicketNotReprintableException extends BusinessException {

    public TicketNotReprintableException() {
        super(HttpStatus.CONFLICT, "booking.ticket-not-reprintable");
    }
}
