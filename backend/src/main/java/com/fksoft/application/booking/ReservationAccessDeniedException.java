package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The caller is neither the reservation owner nor staff (SPEC-0014/0019): 403, business ownership. */
public class ReservationAccessDeniedException extends BusinessException {

    public ReservationAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "booking.not-owner");
    }
}
