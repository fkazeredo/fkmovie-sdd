package com.fksoft.application.screening;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Screening} (SPEC-0009). Module-internal. */
public interface ScreeningRepository extends JpaRepository<Screening, UUID> {

    /** Whether the movie is referenced by any screening — backs the movie deletion guard. */
    boolean existsByMovieId(UUID movieId);

    /** Ids of screenings starting after {@code now} — backs the booking "upcoming" filter (SPEC-0019). */
    @Query("select s.id from Screening s where s.startsAt > :now")
    List<UUID> findIdsByStartsAtAfter(@Param("now") Instant now);

    /**
     * Application-level overlap pre-check (the DB exclusion constraint is the guarantee). Two
     * ranges overlap when {@code startsAt < otherEnd AND endsAt > otherStart}. {@code excludeId}
     * skips the screening being edited (null on create).
     */
    @Query(
            """
            select (count(s) > 0) from Screening s
            where s.roomId = :roomId and s.status = :status
              and (:excludeId is null or s.id <> :excludeId)
              and s.startsAt < :newEnd and s.endsAt > :newStart
            """)
    boolean hasOverlap(
            @Param("roomId") UUID roomId,
            @Param("status") ScreeningStatus status,
            @Param("excludeId") UUID excludeId,
            @Param("newStart") Instant newStart,
            @Param("newEnd") Instant newEnd);

    /**
     * Paginated admin listing with optional room/movie/time-window filters. Each filter uses
     * {@code coalesce(:param, column)} rather than {@code :param is null}: an untyped null bind
     * parameter in a standalone {@code is null} check makes Postgres fail with "could not
     * determine data type". The coalesce anchors each parameter's type to its (NOT NULL) column,
     * and a null filter reduces to {@code column = column} (always true).
     */
    @Query(
            """
            select s from Screening s
            where s.roomId = coalesce(:roomId, s.roomId)
              and s.movieId = coalesce(:movieId, s.movieId)
              and s.startsAt >= coalesce(:from, s.startsAt)
              and s.startsAt <= coalesce(:to, s.startsAt)
            """)
    Page<Screening> search(
            @Param("roomId") UUID roomId,
            @Param("movieId") UUID movieId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /**
     * Public listing (SPEC-0010): SCHEDULED screenings of ACTIVE movies in the future, within an
     * explicit {@code [from, to)} window (the caller passes a wide window when no date filter is set,
     * so no nullable bind appears in a bare {@code is null}; the optional movie uses {@code coalesce}).
     */
    @Query(
            value =
                    """
                    select s from Screening s, Movie m
                    where m.id = s.movieId and m.status = com.fksoft.application.screening.MovieStatus.ACTIVE
                      and s.status = com.fksoft.application.screening.ScreeningStatus.SCHEDULED
                      and s.startsAt > :now and s.startsAt >= :from and s.startsAt < :to
                      and s.movieId = coalesce(:movieId, s.movieId)
                    """,
            countQuery =
                    """
                    select count(s) from Screening s, Movie m
                    where m.id = s.movieId and m.status = com.fksoft.application.screening.MovieStatus.ACTIVE
                      and s.status = com.fksoft.application.screening.ScreeningStatus.SCHEDULED
                      and s.startsAt > :now and s.startsAt >= :from and s.startsAt < :to
                      and s.movieId = coalesce(:movieId, s.movieId)
                    """)
    Page<Screening> searchPublic(
            @Param("now") Instant now,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("movieId") UUID movieId,
            Pageable pageable);
}
