package com.fksoft.domain.payment;

/** Lifecycle of a payment (SPEC-0015): created PENDING, settled asynchronously via webhook. */
public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED
}
