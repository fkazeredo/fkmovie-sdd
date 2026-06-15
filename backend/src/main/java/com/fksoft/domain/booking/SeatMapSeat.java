package com.fksoft.domain.booking;

import com.fksoft.domain.cinema.SeatType;
import java.util.UUID;

/**
 * One seat on the map (SPEC-0011): physical addressing and type from the cinema, availability
 * {@code status} from the screening's inventory, and the full price (flat base until pricing 0012).
 */
public record SeatMapSeat(
        UUID seatId, String row, int number, SeatType type, ScreeningSeatStatus status, int fullPriceCents) {}
