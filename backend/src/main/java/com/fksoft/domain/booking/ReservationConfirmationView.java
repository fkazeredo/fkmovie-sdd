package com.fksoft.domain.booking;

import java.time.Instant;
import java.util.UUID;

/** Response of the confirm endpoint (SPEC-0016): the reservation now AWAITING_PAYMENT, plus the charge. */
public record ReservationConfirmationView(
        UUID reservationId, ReservationStatus status, UUID paymentId, Instant paymentDeadlineAt) {

    static ReservationConfirmationView of(Reservation reservation) {
        return new ReservationConfirmationView(
                reservation.id(), reservation.status(), reservation.paymentId(), reservation.paymentDeadlineAt());
    }
}
