package com.fksoft.application.screening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** SPEC-0008: movie domain invariants and lifecycle transitions. */
class MovieTest {

    @Test
    void buildsAnActiveMovie() {
        var movie = new Movie("Alien", 117, AgeRating.A14, "In space...", "https://img/alien.jpg");

        assertThat(movie.title()).isEqualTo("Alien");
        assertThat(movie.durationMinutes()).isEqualTo(117);
        assertThat(movie.ageRating()).isEqualTo(AgeRating.A14);
        assertThat(movie.status()).isEqualTo(MovieStatus.ACTIVE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankTitle(String title) {
        assertThatThrownBy(() -> new Movie(title, 100, AgeRating.L, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 601})
    void rejectsDurationOutOfRange(int duration) {
        assertThatThrownBy(() -> new Movie("Title", duration, AgeRating.L, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void archiveAndUnarchiveToggleStatus() {
        var movie = new Movie("Title", 100, AgeRating.L, null, null);

        movie.archive();
        assertThat(movie.status()).isEqualTo(MovieStatus.ARCHIVED);

        movie.unarchive();
        assertThat(movie.status()).isEqualTo(MovieStatus.ACTIVE);
    }

    @Test
    void updateReplacesDescriptiveFields() {
        var movie = new Movie("Old", 100, AgeRating.L, "old synopsis", null);

        movie.update("New", 120, AgeRating.A16, "new synopsis", "https://img/new.jpg");

        assertThat(movie.title()).isEqualTo("New");
        assertThat(movie.durationMinutes()).isEqualTo(120);
        assertThat(movie.ageRating()).isEqualTo(AgeRating.A16);
        assertThat(movie.synopsis()).isEqualTo("new synopsis");
        assertThat(movie.posterUrl()).isEqualTo("https://img/new.jpg");
    }
}
