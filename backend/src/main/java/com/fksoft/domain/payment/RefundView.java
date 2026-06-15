package com.fksoft.domain.payment;

/**
 * Stable refund projection exposed to other modules (SPEC-0019): the refund amount and its current
 * settlement status. Part of the payment module's public read API ({@link PaymentLedger}).
 */
public record RefundView(int amountCents, PaymentStatus status) {}
