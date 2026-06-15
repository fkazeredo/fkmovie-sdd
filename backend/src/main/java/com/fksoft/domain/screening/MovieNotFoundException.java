package com.fksoft.domain.screening;

import com.fksoft.domain.error.DomainException;

/** Lookup of a movie that does not exist (SPEC-0008). */
public class MovieNotFoundException extends DomainException {

    public MovieNotFoundException() {
        super("movie.not-found");
    }
}
