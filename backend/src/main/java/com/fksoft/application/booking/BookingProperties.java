package com.fksoft.application.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Reservation tunables (SPEC-0014): seat cap, hold duration and minimum lead time before start. */
@Component
public class BookingProperties {

    private final int maxSeatsPerReservation;
    private final int holdMinutes;
    private final int minLeadMinutes;
    private final int paymentDeadlineMinutes;
    private final int expirationBatchSize;
    private final int cancellationWindowHours;

    BookingProperties(
            @Value("${app.booking.max-seats-per-reservation:8}") int maxSeatsPerReservation,
            @Value("${app.booking.hold-minutes:5}") int holdMinutes,
            @Value("${app.booking.min-lead-minutes:10}") int minLeadMinutes,
            @Value("${app.booking.payment-deadline-minutes:10}") int paymentDeadlineMinutes,
            @Value("${app.booking.expiration-batch-size:100}") int expirationBatchSize,
            @Value("${app.booking.cancellation-window-hours:2}") int cancellationWindowHours) {
        this.maxSeatsPerReservation = maxSeatsPerReservation;
        this.holdMinutes = holdMinutes;
        this.minLeadMinutes = minLeadMinutes;
        this.paymentDeadlineMinutes = paymentDeadlineMinutes;
        this.expirationBatchSize = expirationBatchSize;
        this.cancellationWindowHours = cancellationWindowHours;
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

    public int paymentDeadlineMinutes() {
        return paymentDeadlineMinutes;
    }

    public int expirationBatchSize() {
        return expirationBatchSize;
    }

    public int cancellationWindowHours() {
        return cancellationWindowHours;
    }
}
