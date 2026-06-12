package com.fksoft.application.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Reservation tunables (SPEC-0014): seat cap, hold duration and minimum lead time before start. */
@Component
public class BookingProperties {

    private final int maxSeatsPerReservation;
    private final int holdMinutes;
    private final int minLeadMinutes;

    BookingProperties(
            @Value("${app.booking.max-seats-per-reservation:8}") int maxSeatsPerReservation,
            @Value("${app.booking.hold-minutes:5}") int holdMinutes,
            @Value("${app.booking.min-lead-minutes:10}") int minLeadMinutes) {
        this.maxSeatsPerReservation = maxSeatsPerReservation;
        this.holdMinutes = holdMinutes;
        this.minLeadMinutes = minLeadMinutes;
    }

    public int maxSeatsPerReservation() {
        return maxSeatsPerReservation;
    }

    public int holdMinutes() {
        return holdMinutes;
    }

    public int minLeadMinutes() {
        return minLeadMinutes;
    }
}
