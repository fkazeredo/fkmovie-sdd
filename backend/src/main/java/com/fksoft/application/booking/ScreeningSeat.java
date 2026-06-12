package com.fksoft.application.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * A seat's availability for one screening (SPEC-0009, materialized; read model owned by
 * SPEC-0011). Created {@code FREE}; the {@code FREE → HELD → SOLD} transitions and the
 * pessimistic/optimistic locking (ADR 0004, {@link #version}) arrive with reservations
 * (SPEC-0014).
 */
@Entity
@Table(name = "screening_seats")
public class ScreeningSeat {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "screening_id", nullable = false)
    private UUID screeningId;

    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScreeningSeatStatus status;

    @Version
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScreeningSeat() {
        // JPA
    }

    private ScreeningSeat(UUID screeningId, UUID seatId) {
        this.id = UUID.randomUUID();
        this.tenantId = "default";
        this.screeningId = screeningId;
        this.seatId = seatId;
        this.status = ScreeningSeatStatus.FREE;
    }

    /** Creates a fresh, FREE inventory row for a screening's seat (SPEC-0009). */
    public static ScreeningSeat free(UUID screeningId, UUID seatId) {
        return new ScreeningSeat(screeningId, seatId);
    }

    public boolean isFree() {
        return status == ScreeningSeatStatus.FREE;
    }

    /**
     * Holds the seat for a reservation (SPEC-0014): {@code FREE → HELD}. Callers MUST hold the
     * pessimistic row lock (ADR 0004) and have verified {@link #isFree()} first.
     *
     * @throws IllegalStateException if the seat is not FREE (defensive — the lock + check prevent it).
     */
    public void hold() {
        if (status != ScreeningSeatStatus.FREE) {
            throw new IllegalStateException("Seat " + id + " is not FREE: " + status);
        }
        status = ScreeningSeatStatus.HELD;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public UUID screeningId() {
        return screeningId;
    }

    public UUID seatId() {
        return seatId;
    }

    public ScreeningSeatStatus status() {
        return status;
    }
}
