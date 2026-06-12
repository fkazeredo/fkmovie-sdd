package com.fksoft.application.screening;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The referenced room does not exist (SPEC-0009). */
public class ScreeningRoomNotFoundException extends BusinessException {

    public ScreeningRoomNotFoundException() {
        super(HttpStatus.NOT_FOUND, "screening.room-not-found");
    }
}
