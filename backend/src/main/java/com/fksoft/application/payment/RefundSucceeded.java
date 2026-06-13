package com.fksoft.application.payment;

import java.util.UUID;

/** A refund settled successfully (SPEC-0015). Consumed by cancellation (0018). */
public record RefundSucceeded(UUID paymentId, UUID reservationId, int amountCents) {}
