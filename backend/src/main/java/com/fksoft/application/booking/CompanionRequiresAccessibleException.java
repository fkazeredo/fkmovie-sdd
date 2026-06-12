package com.fksoft.application.booking;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** A COMPANION seat may only be held together with an ACCESSIBLE seat (SPEC-0014). */
public class CompanionRequiresAccessibleException extends BusinessException {

    public CompanionRequiresAccessibleException() {
        super(HttpStatus.CONFLICT, "booking.companion-requires-accessible");
    }
}
