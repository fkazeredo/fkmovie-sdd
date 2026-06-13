package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** A confirmed reservation is too close to the session start to self-cancel (SPEC-0018): 409. */
public class CancellationWindowClosedException extends BusinessException {

    public CancellationWindowClosedException() {
        super(HttpStatus.CONFLICT, "booking.cancellation-window-closed");
    }
}
