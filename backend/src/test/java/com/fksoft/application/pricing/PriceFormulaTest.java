package com.fksoft.application.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** SPEC-0012: the price formula and its rounding edges. */
class PriceFormulaTest {

    @Test
    void fullIsBasePlusSurchargeTimesMultiplierRoundedHalfUp() {
        // Spec example: base 3000 + VIP 1000, Wednesday 0.70 → 4000 × 0.70 = 2800.
        assertThat(PriceFormula.full(3000, 1000, new BigDecimal("0.70"))).isEqualTo(2800);
        // Multiplier 1.00 leaves the price unchanged.
        assertThat(PriceFormula.full(3000, 0, new BigDecimal("1.00"))).isEqualTo(3000);
        // 3575 × 0.70 = 2502.5 → HALF_UP → 2503.
        assertThat(PriceFormula.full(3575, 0, new BigDecimal("0.70"))).isEqualTo(2503);
    }

    @Test
    void halfIsCeilOfHalfFull() {
        assertThat(PriceFormula.half(2800)).isEqualTo(1400);
        // Odd full price rounds the half up to the cent.
        assertThat(PriceFormula.half(2801)).isEqualTo(1401);
        assertThat(PriceFormula.half(3001)).isEqualTo(1501);
    }
}
