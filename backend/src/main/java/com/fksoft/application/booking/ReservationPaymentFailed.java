package com.fksoft.application.booking;

import java.time.Instant;
import java.util.UUID;

/** A reservation was cancelled because its payment failed (SPEC-0016). Audit/metrics. */
public record ReservationPaymentFailed(UUID reservationId, UUID userId, Instant occurredAt) {}
