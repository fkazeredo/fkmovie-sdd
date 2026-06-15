package com.fksoft.domain.screening;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public read API of the screening module for movie descriptive data (SPEC-0019): the synchronous
 * collaboration point other modules use (e.g. booking's reservation views, the public screenings
 * list). Returns a stable {@link MovieView}, never the {@code Movie} entity (ArchUnit-enforced).
 */
@Service
public class MovieCatalog {

    private final MovieRepository movies;

    MovieCatalog(MovieRepository movies) {
        this.movies = movies;
    }

    /** Reads a movie as a stable projection (empty if unknown). */
    @Transactional(readOnly = true)
    public Optional<MovieView> find(UUID movieId) {
        return movies.findById(movieId).map(MovieCatalog::toView);
    }

    /** Batch read for list assembly (SPEC-0019); unknown ids are simply absent. */
    @Transactional(readOnly = true)
    public List<MovieView> findAll(Collection<UUID> movieIds) {
        return movies.findAllById(movieIds).stream().map(MovieCatalog::toView).toList();
    }

    private static MovieView toView(Movie movie) {
        return new MovieView(
                movie.id(),
                movie.title(),
                movie.ageRating(),
                movie.durationMinutes(),
                movie.posterUrl(),
                movie.status());
    }
}
