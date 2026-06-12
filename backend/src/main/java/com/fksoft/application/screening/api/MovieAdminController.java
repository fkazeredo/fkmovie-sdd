package com.fksoft.application.screening.api;

import com.fksoft.application.screening.MovieService;
import com.fksoft.application.screening.MovieStatus;
import com.fksoft.shared.pagination.PageResponse;
import com.fksoft.shared.security.UserContextProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin movie catalog endpoints (SPEC-0008). Access is restricted to ROLE_ADMIN by the security
 * chain ({@code /api/admin/**}); business rules live in {@link MovieService}.
 */
@RestController
@RequestMapping("/api/admin/movies")
class MovieAdminController {

    private final MovieService movieService;
    private final UserContextProvider userContext;

    MovieAdminController(MovieService movieService, UserContextProvider userContext) {
        this.movieService = movieService;
        this.userContext = userContext;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MovieResponse create(@Valid @RequestBody MovieRequest request) {
        var movie = movieService.create(
                request.title(),
                request.durationMinutes(),
                request.ageRating(),
                request.synopsis(),
                request.posterUrl(),
                actingAdminId());
        return MovieResponse.from(movie);
    }

    @GetMapping
    PageResponse<MovieResponse> list(
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = movieService.list(status, search, PageRequest.of(page, size));
        return PageResponse.from(result, MovieResponse::from);
    }

    @GetMapping("/{id}")
    MovieResponse get(@PathVariable UUID id) {
        return MovieResponse.from(movieService.get(id));
    }

    @PutMapping("/{id}")
    MovieResponse update(@PathVariable UUID id, @Valid @RequestBody MovieRequest request) {
        var movie = movieService.update(
                id,
                request.title(),
                request.durationMinutes(),
                request.ageRating(),
                request.synopsis(),
                request.posterUrl(),
                actingAdminId());
        return MovieResponse.from(movie);
    }

    @PostMapping("/{id}/archive")
    MovieResponse archive(@PathVariable UUID id) {
        return MovieResponse.from(movieService.archive(id, actingAdminId()));
    }

    @PostMapping("/{id}/unarchive")
    MovieResponse unarchive(@PathVariable UUID id) {
        return MovieResponse.from(movieService.unarchive(id, actingAdminId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        movieService.delete(id, actingAdminId());
    }

    private UUID actingAdminId() {
        return userContext.currentUser().userId();
    }
}
