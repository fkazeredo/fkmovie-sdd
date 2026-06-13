package com.fksoft.application.booking;

/**
 * Why a reservation was cancelled (SPEC-0017). v1 only the payment-timeout sweep cancels; manual
 * cancellation (SPEC-0018) will add reasons here.
 */
public enum CancellationReason {
    PAYMENT_TIMEOUT
}
