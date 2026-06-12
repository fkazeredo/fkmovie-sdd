package com.fksoft.application.screening;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Guards movie deletion (SPEC-0008): a movie referenced by screenings must be archived, not
 * deleted.
 *
 * <p>Deferred seam (see {@code architecture/simulation-and-mocking.md}): screenings only exist
 * from SPEC-0009, so this guard currently reports no references and permits deletion. SPEC-0009
 * will inject the screening repository here and throw {@link MovieHasScreeningsException} when a
 * screening references the movie — the 409 path and its i18n message are already wired but inert.
 */
@Component
public class MovieDeletionGuard {

    /** No-op until SPEC-0009 wires the real screening-reference check. */
    public void assertDeletable(UUID movieId) {
        // SPEC-0009: throw MovieHasScreeningsException when a screening references this movie.
    }
}
