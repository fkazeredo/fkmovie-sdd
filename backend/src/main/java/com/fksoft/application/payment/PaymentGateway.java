package com.fksoft.application.payment;

/**
 * Provider-agnostic payment port (ADR 0006, SPEC-0015). Returns immediately with a PENDING payment;
 * the final outcome is delivered asynchronously via webhook. This interface is the module's public
 * API — the documented exception to "no interface for a single implementation": the whole point is
 * that a real provider becomes a new adapter, not a domain change.
 */
public interface PaymentGateway {

    PaymentRequestResult request(PaymentRequest request);

    RefundRequestResult requestRefund(RefundRequest request);
}
