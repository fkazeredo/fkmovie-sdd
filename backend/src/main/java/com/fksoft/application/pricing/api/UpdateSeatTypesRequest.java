package com.fksoft.application.pricing.api;

import com.fksoft.application.cinema.SeatType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Bulk update of seat-type surcharges (SPEC-0012). The value rules (≥ 0; ACCESSIBLE/COMPANION = 0)
 * are business errors validated in the service so they surface the {@code pricing.invalid-surcharge}
 * code rather than a generic validation error.
 */
public record UpdateSeatTypesRequest(@NotEmpty @Valid List<Entry> seatTypes) {

    public record Entry(@NotNull SeatType seatType, @NotNull Integer surchargeCents) {}
}
