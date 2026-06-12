package com.fksoft.application.screening;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Deleting a movie that is referenced by screenings (SPEC-0008): it must be archived instead.
 * The 409 path is wired but currently inert — {@link MovieDeletionGuard} starts reporting
 * references only when SPEC-0009 introduces screenings.
 */
public class MovieHasScreeningsException extends BusinessException {

    public MovieHasScreeningsException() {
        super(HttpStatus.CONFLICT, "movie.has-screenings");
    }
}
