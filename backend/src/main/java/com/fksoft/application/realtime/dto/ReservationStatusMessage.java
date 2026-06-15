package com.fksoft.application.realtime.dto;

import com.fksoft.domain.booking.ReservationStatus;
import java.util.UUID;

/**
 * Realtime reservation-status message on the owner's {@code /user/queue/reservations} (SPEC-0013).
 * Stable contract — evolutions add fields, never repurpose.
 */
public record ReservationStatusMessage(String type, UUID reservationId, ReservationStatus status) {

    public static ReservationStatusMessage of(UUID reservationId, ReservationStatus status) {
        return new ReservationStatusMessage("RESERVATION_STATUS_CHANGED", reservationId, status);
    }
}
