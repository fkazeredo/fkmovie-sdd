package com.fksoft.domain.pricing;

import com.fksoft.domain.cinema.SeatType;
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

    /**
     * The cheapest possible full price for a screening (SPEC-0010): the minimum full price across the
     * seat types — the "a partir de" floor shown on the public list. Pure over the config snapshot.
     */
    public int cheapestFull(ScreeningPricingContext ctx) {
        var snapshot = config.current();
        var multiplier = snapshot.multiplier(PricingZone.dayOfWeek(ctx.startsAt()));
        var cheapest = Integer.MAX_VALUE;
        for (var type : SeatType.values()) {
            cheapest =
                    Math.min(cheapest, PriceFormula.full(ctx.basePriceCents(), snapshot.surcharge(type), multiplier));
        }
        return cheapest;
    }
}
