package com.fksoft.application.pricing;

import com.fksoft.application.cinema.SeatType;
import org.springframework.stereotype.Service;

/**
 * Public pricing facade (SPEC-0012): a pure, deterministic quote over the in-memory config
 * snapshot. Same inputs always yield the same output. Consumed by the booking seat map (full
 * price) and reservations (snapshot at hold time).
 */
@Service
public class PriceCalculator {

    private final PricingConfig config;

    PriceCalculator(PricingConfig config) {
        this.config = config;
    }

    /** Quotes a seat: full and half prices plus the amount for {@code ticketType}. */
    public PriceQuote quote(ScreeningPricingContext ctx, SeatType type, TicketType ticketType) {
        var snapshot = config.current();
        var surcharge = snapshot.surcharge(type);
        var multiplier = snapshot.multiplier(PricingZone.dayOfWeek(ctx.startsAt()));
        var full = PriceFormula.full(ctx.basePriceCents(), surcharge, multiplier);
        var half = PriceFormula.half(full);
        var price = ticketType == TicketType.HALF ? half : full;
        return new PriceQuote(full, half, price);
    }
}
