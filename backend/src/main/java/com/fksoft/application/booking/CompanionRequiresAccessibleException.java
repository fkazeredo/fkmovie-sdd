package com.fksoft.application.booking;

import com.fksoft.shared.error.DomainException;

/** A COMPANION seat may only be held together with an ACCESSIBLE seat (SPEC-0014). */
public class CompanionRequiresAccessibleException extends DomainException {

    public CompanionRequiresAccessibleException() {
        super("booking.companion-requires-accessible");
    }
}
