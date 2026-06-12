package com.fksoft.application.pricing;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Weekday multiplier outside the allowed range [0.10, 2.00] (SPEC-0012). */
public class PricingInvalidMultiplierException extends BusinessException {

    public PricingInvalidMultiplierException() {
        super(HttpStatus.BAD_REQUEST, "pricing.invalid-multiplier");
    }
}
