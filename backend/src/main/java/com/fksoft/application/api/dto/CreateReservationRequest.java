package com.fksoft.application.api.dto;

import com.fksoft.domain.pricing.HalfPriceCategory;
import com.fksoft.domain.pricing.TicketType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Reservation request (SPEC-0014): 1–8 seats, each with a ticket type. HALF tickets must carry the
 * declared category and document (cross-field rule checked at the boundary). The seat cap matches
 * {@code app.booking.max-seats-per-reservation} (default 8).
 */
public record CreateReservationRequest(@NotEmpty @Size(max = 8) @Valid List<SeatSelectionRequest> seats) {

    /** One requested seat; for HALF, {@code halfPriceCategory} + {@code documentReference} are required. */
    public record SeatSelectionRequest(
            @NotNull UUID seatId,
            @NotNull TicketType ticketType,
            HalfPriceCategory halfPriceCategory,
            @Size(max = 60) String documentReference) {

        @AssertTrue(message = "HALF tickets require a half-price category and document reference")
        public boolean isHalfPriceComplete() {
            return ticketType != TicketType.HALF
                    || (halfPriceCategory != null && documentReference != null && !documentReference.isBlank());
        }
    }
}
