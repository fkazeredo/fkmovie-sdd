package com.fksoft.application.booking;

import com.fksoft.application.cinema.SeatType;
import org.springframework.stereotype.Component;

/**
 * Computes the full ticket price shown on the seat map (SPEC-0011).
 *
 * <p>Deferred seam (see {@code architecture/simulation-and-mocking.md}): the pricing module
 * (SPEC-0012) owns the real formula — {@code (base + seatTypeSurcharge) * weekdayMultiplier}.
 * Until it exists this returns the screening's flat base price for every seat type (no surcharge,
 * multiplier 1). SPEC-0012 replaces this with a call to the pricing facade. The seat map is
 * informational; reservations (SPEC-0014) snapshot the real computed price.
 */
@Component
public class SeatPricing {

    /** Flat base price for now; SPEC-0012 applies the per-type surcharge and weekday multiplier. */
    public int fullPriceCents(int basePriceCents, SeatType type) {
        return basePriceCents;
    }
}
