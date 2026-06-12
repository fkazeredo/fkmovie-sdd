package com.fksoft.application.screening;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Guards screening edit/cancel against existing reservations and sold tickets (SPEC-0009).
 *
 * <p>Deferred seam (see {@code architecture/simulation-and-mocking.md}): reservations only exist
 * from SPEC-0014, so both checks are no-ops today (edit and cancel are always allowed). SPEC-0014
 * will inject the booking reservation data here and throw
 * {@link ScreeningHasReservationsException} / {@link ScreeningHasSoldTicketsException}; the 409
 * paths and their i18n messages are already wired but inert.
 */
@Component
public class ScreeningModificationGuard {

    /** No-op until SPEC-0014: edit is blocked when the screening has active reservations. */
    public void assertEditable(UUID screeningId) {
        // SPEC-0014: throw ScreeningHasReservationsException when a reservation is PENDING/AWAITING_PAYMENT/CONFIRMED.
    }

    /** No-op until SPEC-0014: cancel is blocked when the screening has sold tickets. */
    public void assertCancellable(UUID screeningId) {
        // SPEC-0014: throw ScreeningHasSoldTicketsException when a CONFIRMED reservation exists.
    }
}
