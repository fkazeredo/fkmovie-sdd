package com.fksoft.application.booking;

import java.time.Instant;
import java.util.UUID;

/**
 * A reservation's status changed (SPEC-0016). Published after commit; the realtime publisher
 * (SPEC-0013) sends it to the owner's private queue. No consumer exists yet.
 */
public record ReservationStatusChanged(UUID reservationId, UUID userId, ReservationStatus status, Instant occurredAt) {}
