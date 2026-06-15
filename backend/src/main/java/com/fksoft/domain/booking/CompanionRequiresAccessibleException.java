package com.fksoft.domain.booking;

import com.fksoft.domain.error.DomainException;

/** A COMPANION seat may only be held together with an ACCESSIBLE seat (SPEC-0014). */
public class CompanionRequiresAccessibleException extends DomainException {

    public CompanionRequiresAccessibleException() {
        super("booking.companion-requires-accessible");
    }
}
