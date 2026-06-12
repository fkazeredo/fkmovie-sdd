package com.fksoft.application.cinema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A physical seat in a {@link CinemaRoom} (SPEC-0007), identified within its room by row
 * (letter) and number. Its {@link SeatType} drives pricing and rendering.
 *
 * <p>A physical seat has NO availability status by design: availability is per-screening and
 * lives in {@code ScreeningSeat} (SPEC-0011). The constructor enforces the row/number format;
 * the {@code UNIQUE (room_id, seat_row, seat_number)} constraint enforces uniqueness.
 */
@Entity
@Table(name = "seats")
public class Seat {

    private static final Pattern ROW_PATTERN = Pattern.compile("^[A-Z]{1,2}$");

    @Id
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "seat_row", nullable = false)
    private String row;

    @Column(name = "seat_number", nullable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Seat() {
        // JPA
    }

    /**
     * Creates a seat, validating the physical addressing rules (SPEC-0007).
     *
     * @throws IllegalArgumentException if the row is not 1-2 uppercase letters or the number
     *     is below 1.
     */
    public Seat(UUID roomId, String row, int number, SeatType type) {
        if (row == null || !ROW_PATTERN.matcher(row).matches()) {
            throw new IllegalArgumentException("Seat row must be 1-2 uppercase letters (A-Z), got: " + row);
        }
        if (number < 1) {
            throw new IllegalArgumentException("Seat number must be >= 1, got: " + number);
        }
        this.id = UUID.randomUUID();
        this.roomId = roomId;
        this.row = row;
        this.number = number;
        this.type = type;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public UUID roomId() {
        return roomId;
    }

    public String row() {
        return row;
    }

    public int number() {
        return number;
    }

    public SeatType type() {
        return type;
    }
}
