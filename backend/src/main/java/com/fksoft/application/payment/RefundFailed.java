package com.fksoft.application.payment;

import java.util.UUID;

/** A refund failed (SPEC-0015). Consumed by cancellation (0018) for follow-up. */
public record RefundFailed(UUID paymentId, UUID reservationId, int amountCents) {}
