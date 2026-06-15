package com.fksoft.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.AbstractIntegrationTest;
import com.fksoft.domain.cinema.CinemaRoomRepository;
import com.fksoft.domain.cinema.SeatRepository;
import com.fksoft.domain.screening.AgeRating;
import com.fksoft.domain.screening.Movie;
import com.fksoft.domain.screening.MovieRepository;
import com.fksoft.domain.screening.Screening;
import com.fksoft.domain.screening.ScreeningRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SPEC-0009: the seat materialization is idempotent (re-running creates no duplicates). */
class ScreeningSeatMaterializationTest extends AbstractIntegrationTest {

    @Autowired
    private ScreeningSeatMaterializer materializer;

    @Autowired
    private ScreeningSeatRepository screeningSeats;

    @Autowired
    private MovieRepository movies;

    @Autowired
    private ScreeningRepository screenings;

    @Autowired
    private CinemaRoomRepository rooms;

    @Autowired
    private SeatRepository seats;

    @AfterEach
    void clean() {
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
    }

    @Test
    void materializeIsIdempotent() {
        var movieId = movies.save(new Movie("Mat Movie", 120, AgeRating.L, null, null))
                .id();
        var roomId = rooms.findByName("Room 4").orElseThrow().id();
        var screeningId = screenings
                .save(Screening.schedule(movieId, roomId, Instant.now().plus(Duration.ofHours(2)), 120, 30, 3000))
                .id();
        var expectedSeats = seats.countByRoomId(roomId);

        materializer.materialize(screeningId, roomId);
        materializer.materialize(screeningId, roomId);

        assertThat(screeningSeats.countByScreeningId(screeningId)).isEqualTo(expectedSeats);
        assertThat(expectedSeats).isEqualTo(48);
    }
}
