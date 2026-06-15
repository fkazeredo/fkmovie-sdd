package com.fksoft.domain.pricing;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Resolves the weekday that drives the multiplier (SPEC-0012): the screening's start day in
 * America/Sao_Paulo. A session at 00:30 Saturday in São Paulo is a Saturday session even though
 * the instant is Friday in UTC.
 */
final class PricingZone {

    static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private PricingZone() {}

    static DayOfWeek dayOfWeek(Instant startsAt) {
        return startsAt.atZone(SAO_PAULO).getDayOfWeek();
    }
}
