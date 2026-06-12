package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Lookup of a reservation that does not exist (SPEC-0014). */
public class ReservationNotFoundException extends BusinessException {

    public ReservationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "reservation.not-found");
    }
}
