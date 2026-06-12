package com.fksoft.application.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** SPEC-0012: the weekday is the screening's start day in America/Sao_Paulo (UTC-3), not UTC. */
class PricingZoneTest {

    @Test
    void usesSaoPauloDayAcrossTheUtcMidnightBoundary() {
        // 2026-01-03 02:00 UTC is still Saturday in UTC, but 23:00 Friday 2026-01-02 in São Paulo.
        assertThat(PricingZone.dayOfWeek(Instant.parse("2026-01-03T02:00:00Z"))).isEqualTo(DayOfWeek.FRIDAY);
        // Control: well inside the day — 12:00 Saturday in São Paulo.
        assertThat(PricingZone.dayOfWeek(Instant.parse("2026-01-03T15:00:00Z"))).isEqualTo(DayOfWeek.SATURDAY);
    }
}
