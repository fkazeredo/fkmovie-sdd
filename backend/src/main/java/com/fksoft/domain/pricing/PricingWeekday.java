package com.fksoft.domain.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Admin-configurable price multiplier for a weekday (SPEC-0012). The key {@code dayOfWeek} is the
 * ISO day number (Monday=1 .. Sunday=7).
 */
@Entity
@Table(name = "pricing_weekday")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PricingWeekday {

    @Id
    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(nullable = false)
    private BigDecimal multiplier;

    PricingWeekday(Integer dayOfWeek, BigDecimal multiplier) {
        this.dayOfWeek = dayOfWeek;
        this.multiplier = multiplier;
    }

    void changeMultiplier(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }
}
