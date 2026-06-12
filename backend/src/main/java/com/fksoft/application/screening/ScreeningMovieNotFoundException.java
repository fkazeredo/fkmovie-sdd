package com.fksoft.application.screening;

import com.fksoft.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

/** The referenced movie does not exist or is not ACTIVE (SPEC-0009): cannot schedule on it. */
public class ScreeningMovieNotFoundException extends BusinessException {

    public ScreeningMovieNotFoundException() {
        super(HttpStatus.NOT_FOUND, "screening.movie-not-found");
    }
}
