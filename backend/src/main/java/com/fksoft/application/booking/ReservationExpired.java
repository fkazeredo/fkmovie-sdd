package com.fksoft.application.booking;

import java.time.Instant;
import java.util.UUID;

/** A PENDING reservation expired and released its seats (SPEC-0017). Audit/metrics; after commit. */
public record ReservationExpired(UUID reservationId, UUID userId, Instant occurredAt) {}
