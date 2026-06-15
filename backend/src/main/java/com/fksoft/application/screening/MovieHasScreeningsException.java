package com.fksoft.application.screening;

import com.fksoft.shared.error.DomainException;

/**
 * Deleting a movie that is referenced by screenings (SPEC-0008): it must be archived instead.
 * The 409 path is wired but currently inert — {@link MovieDeletionGuard} starts reporting
 * references only when SPEC-0009 introduces screenings.
 */
public class MovieHasScreeningsException extends DomainException {

    public MovieHasScreeningsException() {
        super("movie.has-screenings");
    }
}
