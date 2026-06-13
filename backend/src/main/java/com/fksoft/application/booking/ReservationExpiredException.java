package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The reservation hold has expired and cannot be confirmed (SPEC-0016): 410 Gone. */
public class ReservationExpiredException extends BusinessException {

    public ReservationExpiredException() {
        super(HttpStatus.GONE, "booking.reservation-expired");
    }
}
