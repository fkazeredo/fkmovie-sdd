package com.fksoft.application.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.application.cinema.SeatType;
import org.junit.jupiter.api.Test;

/** SPEC-0011: the pricing seam returns the flat base price until pricing (SPEC-0012) lands. */
class SeatPricingTest {

    private final SeatPricing pricing = new SeatPricing();

    @Test
    void returnsFlatBasePriceForEveryType() {
        assertThat(pricing.fullPriceCents(3000, SeatType.STANDARD)).isEqualTo(3000);
        assertThat(pricing.fullPriceCents(3000, SeatType.VIP)).isEqualTo(3000);
        assertThat(pricing.fullPriceCents(3000, SeatType.ACCESSIBLE)).isEqualTo(3000);
    }
}
