package com.fksoft.application.realtime.dto;

import com.fksoft.domain.booking.ScreeningSeatStatus;
import java.util.List;
import java.util.UUID;

/**
 * Realtime seat-update message on {@code /topic/screenings/{id}/seats} (SPEC-0013). A patch of the
 * seats that changed, not the whole map. Stable contract — evolutions add fields, never repurpose.
 */
public record SeatUpdateMessage(String type, UUID screeningId, List<Seat> seats) {

    public record Seat(UUID seatId, String row, int number, ScreeningSeatStatus status) {}

    public static SeatUpdateMessage of(UUID screeningId, List<Seat> seats) {
        return new SeatUpdateMessage("SEAT_STATUS_CHANGED", screeningId, seats);
    }
}
