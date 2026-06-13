package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The reservation was cancelled and cannot be confirmed (SPEC-0016). */
public class ReservationCancelledException extends BusinessException {

    public ReservationCancelledException() {
        super(HttpStatus.CONFLICT, "booking.reservation-cancelled");
    }
}
