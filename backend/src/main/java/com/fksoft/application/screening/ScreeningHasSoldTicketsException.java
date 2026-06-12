package com.fksoft.application.screening;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Cancelling a screening that has sold tickets (SPEC-0009). Wired but inert until reservations
 * exist (SPEC-0014); see {@link ScreeningModificationGuard}.
 */
public class ScreeningHasSoldTicketsException extends BusinessException {

    public ScreeningHasSoldTicketsException() {
        super(HttpStatus.CONFLICT, "screening.has-sold-tickets");
    }
}
