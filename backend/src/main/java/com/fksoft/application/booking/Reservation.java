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

    public List<ReservationSeat> seats() {
        return List.copyOf(seats);
    }
}
