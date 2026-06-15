package com.fksoft.domain.screening;

import com.fksoft.domain.error.DomainException;

/**
 * Editing a screening that already has reservations (SPEC-0009). Wired but inert until
 * reservations exist (SPEC-0014); see {@link ScreeningModificationGuard}.
 */
public class ScreeningHasReservationsException extends DomainException {

    public ScreeningHasReservationsException() {
        super("screening.has-reservations");
    }
}
