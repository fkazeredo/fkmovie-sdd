package com.fksoft.domain.cinema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A physical screening room (SPEC-0007). The cinema has a fixed set of rooms, seed-managed in
 * v1; the room name is unique. Seats belong to a room but are modeled as a separate aggregate
 * member ({@link Seat}) keyed by {@code roomId}, matching the module's plain-UUID reference
 * convention.
 */
@Entity
@Table(name = "cinema_rooms")
public class CinemaRoom {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CinemaRoom() {
        // JPA
    }

    /** Creates a room in the default tenant; rooms are seed-managed in v1. */
    public CinemaRoom(String name) {
        this.id = UUID.randomUUID();
        this.tenantId = "default";
        this.name = name;
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

    public String name() {
        return name;
    }
}
