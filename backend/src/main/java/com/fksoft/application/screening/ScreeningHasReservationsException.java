package com.fksoft.application.screening;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Editing a screening that already has reservations (SPEC-0009). Wired but inert until
 * reservations exist (SPEC-0014); see {@link ScreeningModificationGuard}.
 */
public class ScreeningHasReservationsException extends BusinessException {

    public ScreeningHasReservationsException() {
        super(HttpStatus.CONFLICT, "screening.has-reservations");
    }
}
