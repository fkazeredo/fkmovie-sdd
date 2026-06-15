package com.fksoft.domain.booking;

import java.time.Instant;
import java.util.UUID;

/** A reservation was created (SPEC-0014). Audit/metrics; published after commit. */
public record ReservationCreated(
        UUID reservationId, UUID userId, UUID screeningId, int totalCents, Instant occurredAt) {}
