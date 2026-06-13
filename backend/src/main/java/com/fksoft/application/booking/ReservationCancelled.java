package com.fksoft.application.booking;

import java.time.Instant;
import java.util.UUID;

/** A reservation was cancelled and released its seats (SPEC-0017/0018). Audit/metrics; after commit. */
public record ReservationCancelled(
        UUID reservationId, UUID userId, CancellationReason reason, boolean refundRequested, Instant occurredAt) {}
