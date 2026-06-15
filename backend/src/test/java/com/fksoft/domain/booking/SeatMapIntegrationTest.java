package com.fksoft.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.AbstractIntegrationTest;
import com.fksoft.domain.cinema.CinemaRoomRepository;
import com.fksoft.domain.screening.AgeRating;
import com.fksoft.domain.screening.Movie;
import com.fksoft.domain.screening.MovieRepository;
import com.fksoft.domain.screening.Screening;
import com.fksoft.domain.screening.ScreeningRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SPEC-0011 acceptance: the public seat map of a fresh screening shows the full room FREE. */
class SeatMapIntegrationTest extends AbstractIntegrationTest {

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

    @AfterEach
    void clean() {
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
    }

    @Test
    void returnsFullRoomFreeWithTypesAndPrices() throws Exception {
        var screeningId = materializedScreening("Room 1", 3000);

        var response = get("/api/screenings/" + screeningId + "/seats");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"roomName\":\"Room 1\"")
                .contains("\"status\":\"FREE\"")
                // Pricing (0012) seed: STANDARD/ACCESSIBLE/COMPANION = base 3000; VIP = base + 1000.
                .contains("\"fullPriceCents\":3000")
                .contains("\"fullPriceCents\":4000")
                .contains("\"type\":\"ACCESSIBLE\"")
                .contains("\"type\":\"COMPANION\"")
                .contains("\"type\":\"VIP\"")
                .contains("\"type\":\"STANDARD\"");
        // The status comes from the screening inventory; the physical seat has no status.
        assertThat(response.body()).doesNotContain("\"available\"");
    }

    @Test
    void unknownScreeningReturns404() throws Exception {
        var response = get("/api/screenings/" + UUID.randomUUID() + "/seats");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"code\":\"screening.not-found\"");
    }

    @Test
    void cancelledScreeningReturns410() throws Exception {
        var screeningId = materializedScreening("Room 4", 3000);
        var screening = screenings.findById(screeningId).orElseThrow();
        screening.cancel();
        screenings.save(screening);

        var response = get("/api/screenings/" + screeningId + "/seats");

        assertThat(response.statusCode()).isEqualTo(410);
        assertThat(response.body()).contains("\"code\":\"screening.cancelled\"");
    }

    private UUID materializedScreening(String roomName, int basePriceCents) {
        var movieId = movies.save(new Movie("Map Movie", 120, AgeRating.A14, null, null))
                .id();
        var roomId = rooms.findByName(roomName).orElseThrow().id();
        var screeningId = screenings
                .save(Screening.schedule(
                        movieId, roomId, Instant.now().plus(Duration.ofHours(2)), 120, 30, basePriceCents))
                .id();
        materializer.materialize(screeningId, roomId);
        return screeningId;
    }
}
