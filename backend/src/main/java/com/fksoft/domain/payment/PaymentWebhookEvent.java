package com.fksoft.domain.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Idempotency ledger for processed webhooks (SPEC-0015). A {@code (payment_id, event_type)} is
 * inserted exactly once; the UNIQUE constraint makes a duplicate webhook a no-op.
 */
@Entity
@Table(name = "payment_webhook_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhookEvent {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    PaymentWebhookEvent(UUID paymentId, String eventType, Instant receivedAt) {
        this.id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.receivedAt = receivedAt;
    }
}
