/**
 * Payment module (SPEC-0015, ADR 0006): a provider-agnostic {@link
 * com.fksoft.application.payment.PaymentGateway} port and an async mock adapter that behaves like a
 * real gateway — it settles a charge by delivering a signed webhook to the application's own
 * endpoint, processed idempotently. Swapping to a real provider is a new adapter, not a domain change.
 *
 * <p>Public API: the {@code PaymentGateway} port (request/refund) and the domain events
 * ({@code PaymentSucceeded}, {@code PaymentFailed}, {@code RefundSucceeded}, {@code RefundFailed}).
 * The module owns its ledger and NEVER touches reservations — booking reacts to the events.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Payment")
package com.fksoft.application.payment;
