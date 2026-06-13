package com.fksoft.application.booking;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A customer's hold on one or more seats of a screening (SPEC-0014). Aggregate root over its
 * {@link ReservationSeat}s. Created {@code PENDING} with a 5-minute {@code expiresAt} and the total
 * price snapshotted; {@code version} backs optimistic locking on later status transitions (ADR 0004).
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "screening_id", nullable = false)
    private UUID screeningId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "total_cents", nullable = false)
    private int totalCents;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "payment_deadline_at")
    private Instant paymentDeadlineAt;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "reservation_id", nullable = false)
    private List<ReservationSeat> seats = new ArrayList<>();

    protected Reservation() {
        // JPA
    }

    private Reservation(UUID userId, UUID screeningId, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.tenantId = "default";
        this.userId = userId;
        this.screeningId = screeningId;
        this.status = ReservationStatus.PENDING;
        this.expiresAt = expiresAt;
        this.totalCents = 0;
    }

    /** Opens a PENDING reservation; seats are added with {@link #addSeat}. */
    public static Reservation pending(UUID userId, UUID screeningId, Instant expiresAt) {
        return new Reservation(userId, screeningId, expiresAt);
    }

    /** Adds a held seat and accumulates the total (SPEC-0014). */
    public void addSeat(ReservationSeat seat) {
        seats.add(seat);
        totalCents += seat.priceCents();
    }

    public boolean isPending() {
        return status == ReservationStatus.PENDING;
    }

    public boolean isAwaitingPayment() {
        return status == ReservationStatus.AWAITING_PAYMENT;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    /** Starts payment (SPEC-0016): PENDING → AWAITING_PAYMENT, recording the charge and deadline. */
    public void awaitPayment(UUID paymentId, Instant paymentDeadlineAt) {
        this.status = ReservationStatus.AWAITING_PAYMENT;
        this.paymentId = paymentId;
        this.paymentDeadlineAt = paymentDeadlineAt;
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    /** Expires a stale hold (SPEC-0017): PENDING → EXPIRED. */
    public void expire() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Reservation " + id + " is not PENDING: " + status);
        }
        this.status = ReservationStatus.EXPIRED;
    }

    /** Cancels an unpaid reservation past its deadline (SPEC-0017): AWAITING_PAYMENT → CANCELLED. */
    public void cancelForPaymentTimeout() {
        if (status != ReservationStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Reservation " + id + " is not AWAITING_PAYMENT: " + status);
        }
        this.status = ReservationStatus.CANCELLED;
    }

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public UUID screeningId() {
        return screeningId;
    }

    public ReservationStatus status() {
        return status;
    }

    public int totalCents() {
        return totalCents;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant paymentDeadlineAt() {
        return paymentDeadlineAt;
    }

    public UUID paymentId() {
        return paymentId;
    }

    public List<ReservationSeat> seats() {
        return List.copyOf(seats);
    }
}
