package com.fksoft.domain.screening;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Guards movie deletion (SPEC-0008): a movie referenced by screenings must be archived, not
 * deleted. Harmonized in SPEC-0009 — now that {@code Screening} exists, the check is live
 * (the deferred seam from SPEC-0008 is wired to {@link ScreeningRepository}).
 */
@Component
public class MovieDeletionGuard {

    private final ScreeningRepository screenings;

    MovieDeletionGuard(ScreeningRepository screenings) {
        this.screenings = screenings;
    }

    /**
     * @throws MovieHasScreeningsException if any screening references the movie.
     */
    public void assertDeletable(UUID movieId) {
        if (screenings.existsByMovieId(movieId)) {
            throw new MovieHasScreeningsException();
        }
    }
}
