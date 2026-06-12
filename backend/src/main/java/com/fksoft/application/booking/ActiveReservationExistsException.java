package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The user already holds an active reservation for this screening (SPEC-0014). */
public class ActiveReservationExistsException extends BusinessException {

    public ActiveReservationExistsException() {
        super(HttpStatus.CONFLICT, "booking.active-reservation-exists");
    }
}
