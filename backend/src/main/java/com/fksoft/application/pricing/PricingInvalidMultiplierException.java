package com.fksoft.application.pricing;

import com.fksoft.shared.error.DomainException;

/** Weekday multiplier outside the allowed range [0.10, 2.00] (SPEC-0012). */
public class PricingInvalidMultiplierException extends DomainException {

    public PricingInvalidMultiplierException() {
        super("pricing.invalid-multiplier");
    }
}
