package com.fksoft.domain.pricing;

import java.time.Instant;

/**
 * Inputs a screening contributes to pricing (SPEC-0012): its base price and start instant. The
 * weekday multiplier is keyed by the screening's start day in America/Sao_Paulo (resolved inside
 * the module), not by the purchase day.
 */
public record ScreeningPricingContext(int basePriceCents, Instant startsAt) {}
