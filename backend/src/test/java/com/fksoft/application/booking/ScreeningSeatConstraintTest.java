package com.fksoft.application.booking;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fksoft.AbstractIntegrationTest;
import com.fksoft.application.cinema.CinemaRoomRepository;
import com.fksoft.application.cinema.SeatRepository;
import com.fksoft.application.screening.AgeRating;
import com.fksoft.application.screening.Movie;
import com.fksoft.application.screening.MovieRepository;
import com.fksoft.application.screening.Screening;
import com.fksoft.application.screening.ScreeningRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * SPEC-0011 / ADR 0004: {@code UNIQUE(screening_id, seat_id)} forbids two inventory rows for the
 * same seat in a screening — the first defense against double booking. {@code @Transactional}
 * rolls back so the shared container is untouched.
 */
@Transactional
class ScreeningSeatConstraintTest extends AbstractIntegrationTest {

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

    @Test
    void rejectsDuplicateSeatInSameScreening() {
        var movieId = movies.save(new Movie("C", 120, AgeRating.L, null, null)).id();
        var roomId = rooms.findByName("Room 4").orElseThrow().id();
        var screeningId = screenings
                .save(Screening.schedule(movieId, roomId, Instant.now().plus(Duration.ofHours(2)), 120, 30, 3000))
                .id();
        var seatId = seats.findByRoomId(roomId).get(0).id();
        screeningSeats.saveAndFlush(ScreeningSeat.free(screeningId, seatId));

        assertThatThrownBy(() -> screeningSeats.saveAndFlush(ScreeningSeat.free(screeningId, seatId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
