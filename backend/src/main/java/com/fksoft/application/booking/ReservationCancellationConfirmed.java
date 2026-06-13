package com.fksoft.application.booking;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer cancelled their reservation (SPEC-0018). Carries the recipient's contact (so the
 * notification module needs no auth lookup) and the refund outcome for the cancellation email.
 */
public record ReservationCancellationConfirmed(
        UUID reservationId,
        UUID userId,
        String email,
        String name,
        String preferredLocale,
        boolean refundRequested,
        int refundAmountCents,
        Instant occurredAt) {}
