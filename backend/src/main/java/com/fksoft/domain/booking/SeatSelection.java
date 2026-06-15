package com.fksoft.domain.booking;

import com.fksoft.domain.pricing.HalfPriceCategory;
import com.fksoft.domain.pricing.TicketType;
import java.util.UUID;

/**
 * A seat the customer wants to hold (SPEC-0014): which seat, which ticket type, and — for HALF —
 * the declared half-price category and document. The module-internal form of the request, mapped
 * from the API DTO by the controller.
 */
public record SeatSelection(
        UUID seatId, TicketType ticketType, HalfPriceCategory halfPriceCategory, String documentReference) {}
