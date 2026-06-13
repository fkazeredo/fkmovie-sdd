package com.fksoft.application.payment;

import java.time.Instant;
import java.util.UUID;

/**
 * The signed webhook body exchanged between the mock gateway and the application (SPEC-0015).
 * {@code eventType} is one of {@code PAYMENT_SUCCEEDED|PAYMENT_FAILED|REFUND_SUCCEEDED|REFUND_FAILED}.
 */
public record WebhookPayload(
        UUID eventId, String eventType, UUID paymentId, UUID reservationId, int amountCents, Instant occurredAt) {}
