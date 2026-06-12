package com.fksoft.application.screening;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Lookup of a movie that does not exist (SPEC-0008). */
public class MovieNotFoundException extends BusinessException {

    public MovieNotFoundException() {
        super(HttpStatus.NOT_FOUND, "movie.not-found");
    }
}
