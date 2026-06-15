package com.fksoft.domain.screening;

import com.fksoft.domain.cinema.CinemaCatalog;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin screening management (SPEC-0009): schedule, list/filter, edit, cancel. Validates the
 * movie is ACTIVE (same module) and the room exists (via the cinema {@link CinemaCatalog}
 * facade), enforces the 1-hour-future and room-overlap rules, and publishes {@link
 * ScreeningCreated} so the booking module materializes the seat inventory.
 */
@Service
@Slf4j
public class ScreeningService {

    private static final Duration MIN_LEAD_TIME = Duration.ofHours(1);

    private final ScreeningRepository screenings;
    private final MovieRepository movies;
    private final CinemaCatalog cinema;
    private final ScreeningModificationGuard modificationGuard;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;
    private final int bufferMinutes;

    /** Collaborators injected by Spring — constructor injection only (CLAUDE.md). */
    public ScreeningService(
            ScreeningRepository screenings,
            MovieRepository movies,
            CinemaCatalog cinema,
            ScreeningModificationGuard modificationGuard,
            ApplicationEventPublisher events,
            MeterRegistry meterRegistry,
            @Value("${app.screening.buffer-minutes:30}") int bufferMinutes) {
        this.screenings = screenings;
        this.movies = movies;
        this.cinema = cinema;
        this.modificationGuard = modificationGuard;
        this.events = events;
        this.meterRegistry = meterRegistry;
        this.bufferMinutes = bufferMinutes;
    }

    /** Schedules a screening and publishes {@link ScreeningCreated} (SPEC-0009). */
    @Transactional
    public ScreeningResponse create(
            UUID movieId, UUID roomId, Instant startsAt, int basePriceCents, UUID actingAdminId) {
        var now = Instant.now();
        requireFarEnough(startsAt, now);
        var movie = requireActiveMovie(movieId);
        requireRoom(roomId);
        var screening =
                Screening.schedule(movieId, roomId, startsAt, movie.durationMinutes(), bufferMinutes, basePriceCents);
        requireNoOverlap(roomId, null, screening.startsAt(), screening.endsAt());
        var saved = saveHandlingOverlap(screening);
        events.publishEvent(new ScreeningCreated(saved.id(), saved.roomId(), now));
        audit(actingAdminId, saved.id(), "create");
        return ScreeningResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ScreeningResponse> list(UUID roomId, UUID movieId, Instant from, Instant to, Pageable pageable) {
        return screenings.search(roomId, movieId, from, to, pageable).map(ScreeningResponse::from);
    }

    @Transactional(readOnly = true)
    public ScreeningResponse get(UUID id) {
        return ScreeningResponse.from(find(id));
    }

    /** Edits a screening (SPEC-0009): re-validates refs/overlap and recomputes {@code endsAt}. */
    @Transactional
    public ScreeningResponse update(
            UUID id, UUID movieId, UUID roomId, Instant startsAt, int basePriceCents, UUID actingAdminId) {
        var now = Instant.now();
        var screening = find(id);
        modificationGuard.assertEditable(id);
        requireFarEnough(startsAt, now);
        var movie = requireActiveMovie(movieId);
        requireRoom(roomId);
        screening.reschedule(movieId, roomId, startsAt, movie.durationMinutes(), bufferMinutes, basePriceCents);
        requireNoOverlap(roomId, id, screening.startsAt(), screening.endsAt());
        saveHandlingOverlap(screening);
        audit(actingAdminId, id, "update");
        return ScreeningResponse.from(screening);
    }

    /** Cancels a screening and publishes {@link ScreeningCancelled} (SPEC-0009). */
    @Transactional
    public ScreeningResponse cancel(UUID id, UUID actingAdminId) {
        var screening = find(id);
        modificationGuard.assertCancellable(id);
        screening.cancel();
        events.publishEvent(new ScreeningCancelled(screening.id(), Instant.now()));
        audit(actingAdminId, id, "cancel");
        return ScreeningResponse.from(screening);
    }

    private Screening find(UUID id) {
        return screenings.findById(id).orElseThrow(ScreeningNotFoundException::new);
    }

    private void requireFarEnough(Instant startsAt, Instant now) {
        if (startsAt.isBefore(now.plus(MIN_LEAD_TIME))) {
            throw new ScreeningStartsTooSoonException();
        }
    }

    private Movie requireActiveMovie(UUID movieId) {
        return movies.findById(movieId)
                .filter(movie -> movie.status() == MovieStatus.ACTIVE)
                .orElseThrow(ScreeningMovieNotFoundException::new);
    }

    private void requireRoom(UUID roomId) {
        if (!cinema.roomExists(roomId)) {
            throw new ScreeningRoomNotFoundException();
        }
    }

    private void requireNoOverlap(UUID roomId, UUID excludeId, Instant startsAt, Instant endsAt) {
        if (screenings.hasOverlap(roomId, ScreeningStatus.SCHEDULED, excludeId, startsAt, endsAt)) {
            throw new ScreeningRoomOverlapException();
        }
    }

    private Screening saveHandlingOverlap(Screening screening) {
        try {
            return screenings.saveAndFlush(screening);
        } catch (DataIntegrityViolationException ex) {
            // The GiST exclusion constraint caught a concurrent overlapping insert.
            throw new ScreeningRoomOverlapException();
        }
    }

    private void audit(UUID actingAdminId, UUID screeningId, String action) {
        meterRegistry.counter("admin.screenings.mutations", "action", action).increment();
        log.info("admin action acting={} screeningId={} action={}", actingAdminId, screeningId, action);
    }
}
