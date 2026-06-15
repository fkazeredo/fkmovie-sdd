package com.fksoft.domain.booking;

/**
 * Why a reservation was cancelled: the payment-timeout sweep (SPEC-0017) or the owner's own
 * cancellation (SPEC-0018). Operator/admin cancellation is a future reason.
 */
public enum CancellationReason {
    PAYMENT_TIMEOUT,
    CUSTOMER
}
