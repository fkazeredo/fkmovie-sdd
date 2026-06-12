package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Reservations are closed close to start time (SPEC-0014): {@code startsAt - now < 10 min}. */
public class ScreeningTooSoonException extends BusinessException {

    public ScreeningTooSoonException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "booking.screening-too-soon");
    }
}
