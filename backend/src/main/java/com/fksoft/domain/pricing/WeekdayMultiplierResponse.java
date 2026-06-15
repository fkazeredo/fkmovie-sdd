package com.fksoft.domain.pricing;

import java.math.BigDecimal;

/** Admin view of a weekday multiplier (SPEC-0012); {@code dayOfWeek} is ISO Mon=1..Sun=7. */
public record WeekdayMultiplierResponse(int dayOfWeek, BigDecimal multiplier) {

    static WeekdayMultiplierResponse from(PricingWeekday entity) {
        return new WeekdayMultiplierResponse(entity.dayOfWeek(), entity.multiplier());
    }
}
