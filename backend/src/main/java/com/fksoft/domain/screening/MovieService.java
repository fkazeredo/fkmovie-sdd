package com.fksoft.domain.screening;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin movie catalog management (SPEC-0008): create, list/search, full update, archive/unarchive
 * and delete. Only {@code ADMIN} reaches this service (URL-gated in SecurityConfig); every
 * mutation is audited and metered.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movies;
    private final MovieDeletionGuard deletionGuard;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;

    /** Creates an ACTIVE movie (SPEC-0008). */
    @Transactional
    public MovieResponse create(
            String title,
            int durationMinutes,
            AgeRating ageRating,
            String synopsis,
            String posterUrl,
            UUID actingAdminId) {
        var movie = movies.save(new Movie(title, durationMinutes, ageRating, synopsis, posterUrl));
        audit(actingAdminId, movie.id(), "create");
        return MovieResponse.from(movie);
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> list(MovieStatus status, String search, Pageable pageable) {
        var term = (search == null || search.isBlank()) ? "" : search.trim();
        return movies.search(status, term, pageable).map(MovieResponse::from);
    }

    @Transactional(readOnly = true)
    public MovieResponse get(UUID id) {
        return MovieResponse.from(find(id));
    }

    /** Full update of a movie's descriptive fields (SPEC-0008); status is unchanged. */
    @Transactional
    public MovieResponse update(
            UUID id,
            String title,
            int durationMinutes,
            AgeRating ageRating,
            String synopsis,
            String posterUrl,
            UUID actingAdminId) {
        var movie = find(id);
        movie.update(title, durationMinutes, ageRating, synopsis, posterUrl);
        audit(actingAdminId, id, "update");
        return MovieResponse.from(movie);
    }

    /** Archives a movie and publishes {@link MovieArchived} (SPEC-0008). */
    @Transactional
    public MovieResponse archive(UUID id, UUID actingAdminId) {
        var movie = find(id);
        movie.archive();
        events.publishEvent(new MovieArchived(movie.id(), Instant.now()));
        audit(actingAdminId, id, "archive");
        return MovieResponse.from(movie);
    }

    @Transactional
    public MovieResponse unarchive(UUID id, UUID actingAdminId) {
        var movie = find(id);
        movie.unarchive();
        audit(actingAdminId, id, "unarchive");
        return MovieResponse.from(movie);
    }

    /** Hard-deletes a movie after the screening-reference guard (SPEC-0008; guard inert until 0009). */
    @Transactional
    public void delete(UUID id, UUID actingAdminId) {
        var movie = find(id);
        deletionGuard.assertDeletable(movie.id());
        movies.delete(movie);
        audit(actingAdminId, id, "delete");
    }

    private Movie find(UUID id) {
        return movies.findById(id).orElseThrow(MovieNotFoundException::new);
    }

    private void audit(UUID actingAdminId, UUID movieId, String action) {
        meterRegistry.counter("admin.movies.mutations", "action", action).increment();
        log.info("admin action acting={} movieId={} action={}", actingAdminId, movieId, action);
    }
}
