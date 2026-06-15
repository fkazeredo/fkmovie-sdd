package com.fksoft.domain.payment;

import java.util.UUID;

/**
 * A charge request to the gateway (SPEC-0015). {@code forceOutcome} is a test/staging hook to force
 * the settlement; production callers leave it null and the gateway defaults to SUCCEEDED.
 */
public record PaymentRequest(UUID reservationId, int amountCents, PaymentOutcome forceOutcome) {

    public static PaymentRequest of(UUID reservationId, int amountCents) {
        return new PaymentRequest(reservationId, amountCents, null);
    }
}
