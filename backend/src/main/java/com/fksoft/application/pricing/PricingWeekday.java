package com.fksoft.application.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Admin-configurable price multiplier for a weekday (SPEC-0012). The key {@code dayOfWeek} is the
 * ISO day number (Monday=1 .. Sunday=7).
 */
@Entity
@Table(name = "pricing_weekday")
public class PricingWeekday {

    @Id
    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(nullable = false)
    private BigDecimal multiplier;

    protected PricingWeekday() {
        // JPA
    }

    PricingWeekday(Integer dayOfWeek, BigDecimal multiplier) {
        this.dayOfWeek = dayOfWeek;
        this.multiplier = multiplier;
    }

    void changeMultiplier(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    public Integer dayOfWeek() {
        return dayOfWeek;
    }

    public BigDecimal multiplier() {
        return multiplier;
    }
}
