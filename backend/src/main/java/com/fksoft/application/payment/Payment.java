package com.fksoft.application.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Payment ledger entry (SPEC-0015): a charge or refund, settled asynchronously via webhook. */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    protected Payment() {
        // JPA
    }

    private Payment(UUID reservationId, int amountCents, PaymentKind kind, Instant now) {
        this.id = UUID.randomUUID();
        this.tenantId = "default";
        this.reservationId = reservationId;
        this.amountCents = amountCents;
        this.kind = kind;
        this.status = PaymentStatus.PENDING;
        this.provider = "MOCK";
        this.providerPaymentId = "mock_" + this.id;
        this.createdAt = now;
    }

    public static Payment charge(UUID reservationId, int amountCents, Instant now) {
        return new Payment(reservationId, amountCents, PaymentKind.CHARGE, now);
    }

    public static Payment refund(UUID reservationId, int amountCents, Instant now) {
        return new Payment(reservationId, amountCents, PaymentKind.REFUND, now);
    }

    /** Records the final settlement delivered by the gateway webhook. */
    public void settle(PaymentOutcome outcome, Instant now) {
        this.status = outcome == PaymentOutcome.SUCCEEDED ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        this.settledAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID reservationId() {
        return reservationId;
    }

    public PaymentKind kind() {
        return kind;
    }

    public PaymentStatus status() {
        return status;
    }

    public int amountCents() {
        return amountCents;
    }
}
