package com.fksoft.application.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.application.auth.RegistrationIntegrationTestSupport;
import com.fksoft.application.auth.User;
import com.fksoft.application.auth.UserRepository;
import com.fksoft.application.cinema.CinemaRoomRepository;
import com.fksoft.application.cinema.SeatRepository;
import com.fksoft.application.cinema.SeatType;
import com.fksoft.application.screening.AgeRating;
import com.fksoft.application.screening.Movie;
import com.fksoft.application.screening.MovieRepository;
import com.fksoft.application.screening.Screening;
import com.fksoft.application.screening.ScreeningRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SPEC-0014 acceptance / ADR 0004: many customers hammer the same seat at once; the pessimistic
 * lock guarantees exactly one reservation wins. Scaled down from the spec's 50 threads for test
 * speed — the locking protocol is the same.
 */
class ReservationConcurrencyTest extends RegistrationIntegrationTestSupport {

    private static final int THREADS = 12;

    @Autowired
    private MovieRepository movies;

    @Autowired
    private ScreeningRepository screenings;

    @Autowired
    private ScreeningSeatMaterializer materializer;

    @Autowired
    private ScreeningSeatRepository screeningSeats;

    @Autowired
    private ReservationRepository reservations;

    @Autowired
    private CinemaRoomRepository rooms;

    @Autowired
    private SeatRepository seats;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanBookingData() {
        reservations.deleteAll();
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
    }

    @Test
    void exactlyOneReservationWinsTheSameSeat() throws Exception {
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        var movieId =
                movies.save(new Movie("Hammer", 120, AgeRating.A14, null, null)).id();
        var screeningId = screenings
                .save(Screening.schedule(movieId, roomId, Instant.now().plus(Duration.ofHours(2)), 120, 30, 3000))
                .id();
        materializer.materialize(screeningId, roomId);
        var seatId = seats.findByRoomId(roomId).stream()
                .filter(seat -> seat.type() == SeatType.STANDARD)
                .findFirst()
                .orElseThrow()
                .id();

        var bearers = new ArrayList<String>();
        for (int i = 0; i < THREADS; i++) {
            bearers.add(verifiedCustomerBearer());
        }

        var pool = Executors.newFixedThreadPool(THREADS);
        var ready = new CountDownLatch(THREADS);
        var go = new CountDownLatch(1);
        var futures = new ArrayList<Future<Integer>>();
        for (var bearer : bearers) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return postJson(
                                "/api/screenings/" + screeningId + "/reservations",
                                "{\"seats\":[{\"seatId\":\"%s\",\"ticketType\":\"FULL\"}]}".formatted(seatId),
                                "Authorization",
                                bearer)
                        .statusCode();
            }));
        }
        ready.await();
        go.countDown();

        var statuses = new ArrayList<Integer>();
        for (var future : futures) {
            statuses.add(future.get());
        }
        pool.shutdown();

        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
        assertThat(statuses.stream().filter(s -> s == 409).count()).isEqualTo(THREADS - 1L);
        assertThat(reservations.count()).isEqualTo(1);
        assertThat(screeningSeats.findByScreeningId(screeningId).stream()
                        .filter(seat -> seat.status() == ScreeningSeatStatus.HELD)
                        .count())
                .isEqualTo(1);
    }

    private String verifiedCustomerBearer() throws Exception {
        var email = uniqueEmail();
        var user = User.newCustomer(email, passwordEncoder.encode(PASSWORD), "Customer", "pt-BR");
        user.verifyEmail(Instant.now());
        users.save(user);
        return "Bearer "
                + accessTokenOf(postJson(
                        "/api/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)));
    }
}
