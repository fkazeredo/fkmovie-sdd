package com.fksoft.application.pricing;

import com.fksoft.application.cinema.SeatType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * Immutable snapshot of the pricing configuration (SPEC-0012). Price computation is a pure
 * function over this snapshot, so the hot seat-map path never hits the database per seat.
 */
record PricingSnapshot(Map<SeatType, Integer> surcharges, Map<DayOfWeek, BigDecimal> multipliers) {

    int surcharge(SeatType type) {
        return surcharges.getOrDefault(type, 0);
    }

    BigDecimal multiplier(DayOfWeek day) {
        return multipliers.getOrDefault(day, BigDecimal.ONE);
    }
}
