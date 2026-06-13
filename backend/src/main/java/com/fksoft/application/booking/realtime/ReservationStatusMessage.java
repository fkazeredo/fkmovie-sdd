package com.fksoft.application.booking.realtime;

import com.fksoft.application.booking.ReservationStatus;
import java.util.UUID;

/**
 * Realtime reservation-status message on the owner's {@code /user/queue/reservations} (SPEC-0013).
 * Stable contract — evolutions add fields, never repurpose.
 */
public record ReservationStatusMessage(String type, UUID reservationId, ReservationStatus status) {

    static ReservationStatusMessage of(UUID reservationId, ReservationStatus status) {
        return new ReservationStatusMessage("RESERVATION_STATUS_CHANGED", reservationId, status);
    }
}
