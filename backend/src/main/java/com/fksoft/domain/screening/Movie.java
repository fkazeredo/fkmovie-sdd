package com.fksoft.domain.screening;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A catalog movie (SPEC-0008). Descriptive data managed by admins; {@code durationMinutes}
 * feeds screening overlap validation (0009). The entity guards its core invariants (title
 * present and bounded, duration within range) mirroring the DB CHECK constraints.
 */
@Entity
@Table(name = "movies")
public class Movie {

    private static final int TITLE_MAX = 200;
    private static final int DURATION_MIN = 1;
    private static final int DURATION_MAX = 600;

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String title;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    private String synopsis;

    @Column(name = "poster_url")
    private String posterUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_rating", nullable = false)
    private AgeRating ageRating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovieStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Movie() {
        // JPA
    }

    /** Creates an ACTIVE movie in the default tenant. */
    public Movie(String title, int durationMinutes, AgeRating ageRating, String synopsis, String posterUrl) {
        this.id = UUID.randomUUID();
        this.tenantId = "default";
        this.status = MovieStatus.ACTIVE;
        applyDetails(title, durationMinutes, ageRating, synopsis, posterUrl);
    }

    /** Replaces the descriptive fields (SPEC-0008 full update); status is unchanged. */
    public void update(String title, int durationMinutes, AgeRating ageRating, String synopsis, String posterUrl) {
        applyDetails(title, durationMinutes, ageRating, synopsis, posterUrl);
    }

    public void archive() {
        this.status = MovieStatus.ARCHIVED;
    }

    public void unarchive() {
        this.status = MovieStatus.ACTIVE;
    }

    private void applyDetails(
            String title, int durationMinutes, AgeRating ageRating, String synopsis, String posterUrl) {
        if (title == null || title.isBlank() || title.length() > TITLE_MAX) {
            throw new IllegalArgumentException("Movie title must be 1-" + TITLE_MAX + " characters");
        }
        if (durationMinutes < DURATION_MIN || durationMinutes > DURATION_MAX) {
            throw new IllegalArgumentException(
                    "Movie duration must be between " + DURATION_MIN + " and " + DURATION_MAX + " minutes");
        }
        if (ageRating == null) {
            throw new IllegalArgumentException("Movie age rating is required");
        }
        this.title = title.trim();
        this.durationMinutes = durationMinutes;
        this.ageRating = ageRating;
        this.synopsis = synopsis;
        this.posterUrl = posterUrl;
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

    public String title() {
        return title;
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public String synopsis() {
        return synopsis;
    }

    public String posterUrl() {
        return posterUrl;
    }

    public AgeRating ageRating() {
        return ageRating;
    }

    public MovieStatus status() {
        return status;
    }
}
