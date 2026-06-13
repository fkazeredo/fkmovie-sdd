package com.fksoft.application.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.application.auth.RegistrationIntegrationTestSupport;
import com.fksoft.application.auth.User;
import com.fksoft.application.auth.UserRepository;
import com.fksoft.application.cinema.CinemaRoomRepository;
import com.fksoft.application.cinema.SeatRepository;
import com.fksoft.application.cinema.SeatType;
import com.fksoft.application.payment.MockPaymentJobRepository;
import com.fksoft.application.payment.PaymentRepository;
import com.fksoft.application.screening.AgeRating;
import com.fksoft.application.screening.Movie;
import com.fksoft.application.screening.MovieRepository;
import com.fksoft.application.screening.Screening;
import com.fksoft.application.screening.ScreeningRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

/** SPEC-0016: re-confirming an AWAITING_PAYMENT reservation is idempotent (same payment, no charge). */
// A long mock delay keeps the webhook from settling during the test, so the reservation stays AWAITING_PAYMENT.
@TestPropertySource(properties = "app.payment.mock.delay=PT5M")
class ReservationReconfirmTest extends RegistrationIntegrationTestSupport {

    private static final Pattern PAYMENT_ID = Pattern.compile("\"paymentId\":\"([^\"]+)\"");
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

    @Autowired
    private PaymentRepository payments;

    @Autowired
    private MockPaymentJobRepository jobs;

    @AfterEach
    void clean() {
        reservations.deleteAll();
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
        jobs.deleteAll();
        payments.deleteAll();
    }

    @Test
    void reconfirmReturnsTheSamePayment() throws Exception {
        var movieId =
                movies.save(new Movie("Re", 120, AgeRating.A14, null, null)).id();
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
        var reservationId = group(
                RESERVATION_ID,
                postJson(
                                "/api/screenings/" + screeningId + "/reservations",
                                "{\"seats\":[{\"seatId\":\"%s\",\"ticketType\":\"FULL\"}]}".formatted(seatId),
                                "Authorization",
                                bearer)
                        .body());

        var first = postJson("/api/reservations/" + reservationId + "/confirm", "", "Authorization", bearer);
        var second = postJson("/api/reservations/" + reservationId + "/confirm", "", "Authorization", bearer);

        assertThat(first.statusCode()).isEqualTo(202);
        assertThat(second.statusCode()).isEqualTo(202);
        assertThat(group(PAYMENT_ID, second.body())).isEqualTo(group(PAYMENT_ID, first.body()));
        assertThat(payments.count()).isEqualTo(1);
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

    private String group(Pattern pattern, String body) {
        var matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Pattern " + pattern + " not in: " + body);
        }
        return matcher.group(1);
    }
}
