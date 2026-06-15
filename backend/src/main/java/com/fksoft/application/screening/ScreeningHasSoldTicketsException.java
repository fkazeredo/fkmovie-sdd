package com.fksoft.application.screening;

import com.fksoft.shared.error.DomainException;

/**
 * Cancelling a screening that has sold tickets (SPEC-0009). Wired but inert until reservations
 * exist (SPEC-0014); see {@link ScreeningModificationGuard}.
 */
public class ScreeningHasSoldTicketsException extends DomainException {

    public ScreeningHasSoldTicketsException() {
        super("screening.has-sold-tickets");
    }
}
