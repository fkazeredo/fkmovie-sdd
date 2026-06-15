package com.fksoft.domain.screening;

import com.fksoft.domain.error.DomainException;

/** The referenced room does not exist (SPEC-0009). */
public class ScreeningRoomNotFoundException extends DomainException {

    public ScreeningRoomNotFoundException() {
        super("screening.room-not-found");
    }
}
