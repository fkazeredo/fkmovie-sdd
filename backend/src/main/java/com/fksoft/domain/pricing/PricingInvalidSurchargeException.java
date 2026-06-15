package com.fksoft.domain.pricing;

import com.fksoft.domain.error.DomainException;

/** Invalid seat-type surcharge (SPEC-0012): negative, or non-zero for ACCESSIBLE/COMPANION. */
public class PricingInvalidSurchargeException extends DomainException {

    public PricingInvalidSurchargeException() {
        super("pricing.invalid-surcharge");
    }
}
