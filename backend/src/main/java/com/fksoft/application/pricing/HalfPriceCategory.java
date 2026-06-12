package com.fksoft.application.pricing;

/**
 * Legal half-price eligibility category (Lei 12.933, SPEC-0012/0014). Declared by the customer at
 * reservation and stored for the law's physical-verification model; the system does not validate
 * eligibility online. Does not affect the amount (half is always 50%).
 */
public enum HalfPriceCategory {
    STUDENT,
    ELDERLY,
    PCD,
    PCD_COMPANION,
    LOW_INCOME_YOUTH
}
