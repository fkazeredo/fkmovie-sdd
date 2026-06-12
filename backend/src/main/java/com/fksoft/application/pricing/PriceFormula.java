package com.fksoft.application.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure price arithmetic (SPEC-0012): {@code full = round_half_up((base + surcharge) * multiplier)}
 * and {@code half = ceil(full / 2)} (Lei 12.933 — 50% rounded up to the cent). All values are
 * integer cents.
 */
final class PriceFormula {

    private PriceFormula() {}

    static int full(int baseCents, int surchargeCents, BigDecimal multiplier) {
        return BigDecimal.valueOf((long) baseCents + surchargeCents)
                .multiply(multiplier)
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    /** Ceil of half, so an odd full price rounds the half up to the cent. */
    static int half(int fullCents) {
        return (fullCents + 1) / 2;
    }
}
