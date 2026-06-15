package com.fksoft.domain.payment;

/** Final settlement outcome the gateway delivers via webhook (SPEC-0015). */
public enum PaymentOutcome {
    SUCCEEDED,
    FAILED
}
