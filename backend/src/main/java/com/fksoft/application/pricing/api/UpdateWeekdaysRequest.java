package com.fksoft.application.pricing.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * Bulk update of weekday multipliers (SPEC-0012); {@code dayOfWeek} is ISO Mon=1..Sun=7. The
 * multiplier range [0.10, 2.00] is a business error validated in the service so it surfaces the
 * {@code pricing.invalid-multiplier} code rather than a generic validation error.
 */
public record UpdateWeekdaysRequest(@NotEmpty @Valid List<Entry> weekdays) {

    public record Entry(@NotNull @Min(1) @Max(7) Integer dayOfWeek, @NotNull BigDecimal multiplier) {}
}
