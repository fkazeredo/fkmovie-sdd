package com.fksoft.application.booking;

import java.util.UUID;

/**
 * Response of the cancel endpoint (SPEC-0018): the now-CANCELLED reservation and whether a refund was
 * requested (only for confirmed cancellations). The refund settles asynchronously via the gateway.
 */
public record CancellationView(UUID reservationId, ReservationStatus status, RefundInfo refund) {

    /** Refund summary: {@code requested=false} (amount 0) when no payment was involved. */
    public record RefundInfo(boolean requested, int amountCents) {

        static RefundInfo none() {
            return new RefundInfo(false, 0);
        }

        static RefundInfo of(int amountCents) {
            return new RefundInfo(true, amountCents);
        }
    }
}
