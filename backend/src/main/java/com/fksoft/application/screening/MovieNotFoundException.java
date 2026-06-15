package com.fksoft.application.screening;

import com.fksoft.shared.error.DomainException;

/** Lookup of a movie that does not exist (SPEC-0008). */
public class MovieNotFoundException extends DomainException {

    public MovieNotFoundException() {
        super("movie.not-found");
    }
}
