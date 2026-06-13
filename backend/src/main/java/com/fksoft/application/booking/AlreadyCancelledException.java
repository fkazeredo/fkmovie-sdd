package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The reservation is already CANCELLED — nothing to cancel (SPEC-0018): 409. */
public class AlreadyCancelledException extends BusinessException {

    public AlreadyCancelledException() {
        super(HttpStatus.CONFLICT, "booking.already-cancelled");
    }
}
