package com.fksoft.domain.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A scheduled webhook delivery for the mock gateway (SPEC-0015, ADR 0006). The worker delivers the
 * chosen {@code outcome} once {@code deliverAt} has passed, then stamps {@code deliveredAt}.
 */
@Entity
@Table(name = "mock_payment_jobs")
public class MockPaymentJob {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "deliver_at", nullable = false)
    private Instant deliverAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentOutcome outcome;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected MockPaymentJob() {
        // JPA
    }

    public MockPaymentJob(UUID paymentId, Instant deliverAt, PaymentOutcome outcome) {
        this.id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.deliverAt = deliverAt;
        this.outcome = outcome;
    }

    public void markDelivered(Instant now) {
        this.deliveredAt = now;
    }

    public boolean isDelivered() {
        return deliveredAt != null;
    }

    public UUID id() {
        return id;
    }

    public UUID paymentId() {
        return paymentId;
    }

    public PaymentOutcome outcome() {
        return outcome;
    }
}
