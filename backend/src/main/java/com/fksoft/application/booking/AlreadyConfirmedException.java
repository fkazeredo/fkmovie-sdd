package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The reservation is already confirmed (SPEC-0016). */
public class AlreadyConfirmedException extends BusinessException {

    public AlreadyConfirmedException() {
        super(HttpStatus.CONFLICT, "booking.already-confirmed");
    }
}
