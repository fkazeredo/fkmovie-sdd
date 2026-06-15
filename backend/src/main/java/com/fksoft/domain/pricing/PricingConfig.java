package com.fksoft.domain.pricing;

import jakarta.annotation.PostConstruct;
import java.time.DayOfWeek;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Holds the pricing configuration in memory (SPEC-0012) so {@link PriceCalculator} is a pure,
 * cheap computation on a hot path. Loaded at startup (after Flyway) and reloaded after an admin
 * change commits ({@code PricingConfigChanged} listener). The reference is {@code volatile}, so
 * readers always see a consistent, fully-built snapshot.
 */
@Component
@RequiredArgsConstructor
class PricingConfig {

    private final PricingSeatTypeRepository seatTypes;
    private final PricingWeekdayRepository weekdays;

    private volatile PricingSnapshot snapshot = new PricingSnapshot(Map.of(), Map.of());

    @PostConstruct
    void load() {
        reload();
    }

    @Transactional(readOnly = true)
    void reload() {
        var surcharges = seatTypes.findAll().stream()
                .collect(Collectors.toMap(PricingSeatType::seatType, PricingSeatType::surchargeCents));
        var multipliers = weekdays.findAll().stream()
                .collect(Collectors.toMap(w -> DayOfWeek.of(w.dayOfWeek()), PricingWeekday::multiplier));
        this.snapshot = new PricingSnapshot(surcharges, multipliers);
    }

    PricingSnapshot current() {
        return snapshot;
    }
}
