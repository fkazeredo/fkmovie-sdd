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
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

/** SPEC-0016: a reservation past its hold expiry cannot be confirmed, even if unswept (410). */
// hold-minutes=0 makes the reservation expire the instant it is created.
@TestPropertySource(properties = "app.booking.hold-minutes=0")
class ReservationConfirmExpiryTest extends RegistrationIntegrationTestSupport {

    private static final Pattern RESERVATION_ID = Pattern.compile("\"reservationId\":\"([^\"]+)\"");

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
    void clean() {
        reservations.deleteAll();
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
    }

    @Test
    void confirmingAnExpiredReservationReturns410() throws Exception {
        var movieId =
                movies.save(new Movie("Exp", 120, AgeRating.A14, null, null)).id();
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        var screeningId = screenings
                .save(Screening.schedule(movieId, roomId, Instant.now().plus(Duration.ofHours(2)), 120, 30, 3000))
                .id();
        materializer.materialize(screeningId, roomId);
        var seatId = seats.findByRoomId(roomId).stream()
                .filter(seat -> seat.type() == SeatType.STANDARD)
                .findFirst()
                .orElseThrow()
                .id();
        var bearer = verifiedCustomerBearer();
        var reserve = postJson(
                "/api/screenings/" + screeningId + "/reservations",
                "{\"seats\":[{\"seatId\":\"%s\",\"ticketType\":\"FULL\"}]}".formatted(seatId),
                "Authorization",
                bearer);
        var matcher = RESERVATION_ID.matcher(reserve.body());
        matcher.find();
        var reservationId = matcher.group(1);
        Thread.sleep(50); // ensure now is past the (zero-minute) hold expiry

        var confirm = postJson("/api/reservations/" + reservationId + "/confirm", "", "Authorization", bearer);

        assertThat(confirm.statusCode()).isEqualTo(410);
        assertThat(confirm.body()).contains("\"code\":\"booking.reservation-expired\"");
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
