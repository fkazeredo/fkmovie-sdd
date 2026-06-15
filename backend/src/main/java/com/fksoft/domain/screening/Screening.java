package com.fksoft.domain.screening;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A movie scheduled in a room at a time (SPEC-0009). {@code endsAt} is derived and stored
 * ({@code startsAt + movie duration + cleaning buffer}); room overlap is guaranteed by a DB
 * exclusion constraint. The 1-hour-future rule is enforced by the service (it owns the clock).
 */
@Entity
@Table(name = "screenings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Screening {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "movie_id", nullable = false)
    private UUID movieId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "base_price_cents", nullable = false)
    private int basePriceCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScreeningStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Screening(
            UUID movieId, UUID roomId, Instant startsAt, int durationMinutes, int bufferMinutes, int basePriceCents) {
        this.id = UUID.randomUUID();
        this.tenantId = "default";
        this.status = ScreeningStatus.SCHEDULED;
        apply(movieId, roomId, startsAt, durationMinutes, bufferMinutes, basePriceCents);
    }

    /** Schedules a new screening; {@code endsAt} = start + duration + buffer. */
    public static Screening schedule(
            UUID movieId, UUID roomId, Instant startsAt, int durationMinutes, int bufferMinutes, int basePriceCents) {
        return new Screening(movieId, roomId, startsAt, durationMinutes, bufferMinutes, basePriceCents);
    }

    /** Re-applies the schedulable fields on edit (SPEC-0009 PUT); recomputes {@code endsAt}. */
    public void reschedule(
            UUID movieId, UUID roomId, Instant startsAt, int durationMinutes, int bufferMinutes, int basePriceCents) {
        apply(movieId, roomId, startsAt, durationMinutes, bufferMinutes, basePriceCents);
    }

    public void cancel() {
        this.status = ScreeningStatus.CANCELLED;
    }

    public boolean isScheduled() {
        return status == ScreeningStatus.SCHEDULED;
    }

    private void apply(
            UUID movieId, UUID roomId, Instant startsAt, int durationMinutes, int bufferMinutes, int basePriceCents) {
        if (basePriceCents <= 0) {
            throw new IllegalArgumentException("Screening base price must be positive");
        }
        this.movieId = movieId;
        this.roomId = roomId;
        this.startsAt = startsAt;
        this.endsAt = startsAt.plus(Duration.ofMinutes((long) durationMinutes + bufferMinutes));
        this.basePriceCents = basePriceCents;
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
}
