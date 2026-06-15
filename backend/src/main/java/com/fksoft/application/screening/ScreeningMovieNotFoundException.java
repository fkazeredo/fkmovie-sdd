package com.fksoft.application.screening;

import com.fksoft.shared.error.DomainException;

/** The referenced movie does not exist or is not ACTIVE (SPEC-0009): cannot schedule on it. */
public class ScreeningMovieNotFoundException extends DomainException {

    public ScreeningMovieNotFoundException() {
        super("screening.movie-not-found");
    }
}
