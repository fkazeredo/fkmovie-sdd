package com.fksoft.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.domain.auth.RegistrationIntegrationTestSupport;
import com.fksoft.domain.auth.User;
import com.fksoft.domain.auth.UserRepository;
import com.fksoft.domain.cinema.CinemaRoomRepository;
import com.fksoft.domain.cinema.SeatRepository;
import com.fksoft.domain.cinema.SeatType;
import com.fksoft.domain.screening.AgeRating;
import com.fksoft.domain.screening.Movie;
import com.fksoft.domain.screening.MovieRepository;
import com.fksoft.domain.screening.Screening;
import com.fksoft.domain.screening.ScreeningRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.event.TransactionalEventListener;

/** SPEC-0014: {@code SeatsStatusChanged} is published AFTER commit (for realtime 0013), not on failure. */
@Import(ReservationEventTest.CaptorConfig.class)
class ReservationEventTest extends RegistrationIntegrationTestSupport {

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

    @Autowired
    private SeatsStatusCaptor captor;

    @AfterEach
    void cleanBookingData() {
        reservations.deleteAll();
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
        captor.events.clear();
    }

    @Test
    void publishesHeldAfterCommitAndNotOnFailure() throws Exception {
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        var movieId =
                movies.save(new Movie("Evt", 120, AgeRating.A14, null, null)).id();
        var screeningId = screenings
                .save(Screening.schedule(movieId, roomId, Instant.now().plus(Duration.ofHours(2)), 120, 30, 3000))
                .id();
        materializer.materialize(screeningId, roomId);
        var seatId = seats.findByRoomId(roomId).stream()
                .filter(seat -> seat.type() == SeatType.STANDARD)
                .findFirst()
                .orElseThrow()
                .id();
        captor.events.clear();

        var created = reserve(verifiedCustomerBearer(), screeningId, seatId);
        assertThat(created.statusCode()).isEqualTo(201);

        assertThat(captor.events).hasSize(1);
        assertThat(captor.events.get(0).status()).isEqualTo(ScreeningSeatStatus.HELD);
        assertThat(captor.events.get(0).seatIds()).contains(seatId);

        // A second reservation of the held seat fails and publishes nothing.
        assertThat(reserve(verifiedCustomerBearer(), screeningId, seatId).statusCode())
                .isEqualTo(409);
        assertThat(captor.events).hasSize(1);
    }

    private java.net.http.HttpResponse<String> reserve(String bearer, UUID screeningId, UUID seatId) throws Exception {
        return postJson(
                "/api/screenings/" + screeningId + "/reservations",
                "{\"seats\":[{\"seatId\":\"%s\",\"ticketType\":\"FULL\"}]}".formatted(seatId),
                "Authorization",
                bearer);
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

    @TestConfiguration
    static class CaptorConfig {
        @Bean
        SeatsStatusCaptor seatsStatusCaptor() {
            return new SeatsStatusCaptor();
        }
    }

    static class SeatsStatusCaptor {
        private final List<SeatsStatusChanged> events = new CopyOnWriteArrayList<>();

        @TransactionalEventListener
        void on(SeatsStatusChanged event) {
            events.add(event);
        }
    }
}
