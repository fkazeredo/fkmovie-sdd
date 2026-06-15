package com.fksoft.domain.screening;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public read API of the screening module (SPEC-0011): the synchronous collaboration point other
 * modules use to read a screening (e.g. the booking seat map). Returns a stable {@link
 * ScreeningView}, never the {@code Screening} entity (ArchUnit-enforced).
 */
@Service
@RequiredArgsConstructor
public class ScreeningCatalog {

    private final ScreeningRepository screenings;

    /** Reads a screening as a stable projection (empty if unknown). */
    @Transactional(readOnly = true)
    public Optional<ScreeningView> find(UUID screeningId) {
        return screenings.findById(screeningId).map(ScreeningCatalog::toView);
    }

    /** Batch read for list assembly (SPEC-0019); unknown ids are simply absent. */
    @Transactional(readOnly = true)
    public List<ScreeningView> findAll(Collection<UUID> screeningIds) {
        return screenings.findAllById(screeningIds).stream()
                .map(ScreeningCatalog::toView)
                .toList();
    }

    /** Ids of screenings starting after {@code now} — backs the booking "upcoming" filter (SPEC-0019). */
    @Transactional(readOnly = true)
    public List<UUID> futureScreeningIds(Instant now) {
        return screenings.findIdsByStartsAtAfter(now);
    }

    private static ScreeningView toView(Screening s) {
        return new ScreeningView(
                s.id(), s.movieId(), s.roomId(), s.startsAt(), s.endsAt(), s.basePriceCents(), s.status());
    }
}
