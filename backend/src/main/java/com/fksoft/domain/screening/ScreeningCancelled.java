package com.fksoft.domain.screening;

import java.time.Instant;
import java.util.UUID;

/**
 * A screening was cancelled (SPEC-0009). Public module event. In v1 the booking module takes no
 * action on it (the CANCELLED status itself blocks new reservations; no sold tickets by rule).
 */
public record ScreeningCancelled(UUID screeningId, Instant occurredAt) {}
