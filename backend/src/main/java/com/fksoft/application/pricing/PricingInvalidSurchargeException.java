package com.fksoft.application.pricing;

import com.fksoft.shared.error.DomainException;

/** Invalid seat-type surcharge (SPEC-0012): negative, or non-zero for ACCESSIBLE/COMPANION. */
public class PricingInvalidSurchargeException extends DomainException {

    public PricingInvalidSurchargeException() {
        super("pricing.invalid-surcharge");
    }
}
