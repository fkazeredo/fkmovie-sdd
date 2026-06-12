package com.fksoft.application.pricing;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Invalid seat-type surcharge (SPEC-0012): negative, or non-zero for ACCESSIBLE/COMPANION. */
public class PricingInvalidSurchargeException extends BusinessException {

    public PricingInvalidSurchargeException() {
        super(HttpStatus.BAD_REQUEST, "pricing.invalid-surcharge");
    }
}
