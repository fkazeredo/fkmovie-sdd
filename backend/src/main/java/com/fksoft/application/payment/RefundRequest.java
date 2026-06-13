package com.fksoft.application.payment;

import java.util.UUID;

/** A refund request to the gateway (SPEC-0015), consumed by cancellation (0018) and late success. */
public record RefundRequest(UUID reservationId, int amountCents) {}
