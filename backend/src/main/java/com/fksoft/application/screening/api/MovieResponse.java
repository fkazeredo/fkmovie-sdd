package com.fksoft.application.screening.api;

import com.fksoft.application.screening.AgeRating;
import com.fksoft.application.screening.Movie;
import com.fksoft.application.screening.MovieStatus;
import java.util.UUID;

/** Admin-facing movie view (SPEC-0008). */
public record MovieResponse(
        UUID id,
        String title,
        int durationMinutes,
        String synopsis,
        String posterUrl,
        AgeRating ageRating,
        MovieStatus status) {

    /** Maps a movie entity to its admin-facing view. */
    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
                movie.id(),
                movie.title(),
                movie.durationMinutes(),
                movie.synopsis(),
                movie.posterUrl(),
                movie.ageRating(),
                movie.status());
    }
}
