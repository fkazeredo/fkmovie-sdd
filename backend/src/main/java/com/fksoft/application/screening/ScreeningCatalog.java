package com.fksoft.application.screening;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public read API of the screening module (SPEC-0011): the synchronous collaboration point other
 * modules use to read a screening (e.g. the booking seat map). Returns a stable {@link
 * ScreeningView}, never the {@code Screening} entity (ArchUnit-enforced).
 */
@Service
public class ScreeningCatalog {

    private final ScreeningRepository screenings;

    ScreeningCatalog(ScreeningRepository screenings) {
        this.screenings = screenings;
    }

    /** Reads a screening as a stable projection (empty if unknown). */
    @Transactional(readOnly = true)
    public Optional<ScreeningView> find(UUID screeningId) {
        return screenings
                .findById(screeningId)
                .map(s -> new ScreeningView(
                        s.id(), s.movieId(), s.roomId(), s.startsAt(), s.endsAt(), s.basePriceCents(), s.status()));
    }
}
