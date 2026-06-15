package com.fksoft.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.domain.auth.RegistrationIntegrationTestSupport;
import com.fksoft.domain.auth.Role;
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
import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/** SPEC-0014 acceptance: a verified customer holds FREE seats, all-or-nothing, price snapshotted. */
class ReservationIntegrationTest extends RegistrationIntegrationTestSupport {

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
    void cleanBookingData() {
        reservations.deleteAll();
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
    }

    @Test
    void reservesSeatsWithSnapshotPrices() throws Exception {
        var screeningId = materializedScreening(future(2));
        var bearer = verifiedCustomerBearer();
        var standards = seatsOfType(SeatType.STANDARD, 2);

        var response = reserve(bearer, screeningId, full(standards.get(0)) + "," + half(standards.get(1)));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body())
                .contains("\"status\":\"PENDING\"")
                .contains("\"expiresAt\":\"")
                .contains("\"totalCents\":4500") // 3000 (full) + 1500 (half)
                .contains("\"priceCents\":3000")
                .contains("\"priceCents\":1500");
    }

    @Test
    void allOrNothingWhenASeatIsAlreadyHeld() throws Exception {
        var screeningId = materializedScreening(future(2));
        var standards = seatsOfType(SeatType.STANDARD, 2);
        assertThat(reserve(verifiedCustomerBearer(), screeningId, full(standards.get(0)))
                        .statusCode())
                .isEqualTo(201);

        var conflict =
                reserve(verifiedCustomerBearer(), screeningId, full(standards.get(0)) + "," + full(standards.get(1)));

        assertThat(conflict.statusCode()).isEqualTo(409);
        assertThat(conflict.body())
                .contains("\"code\":\"booking.seats-unavailable\"")
                .contains(standards.get(0).toString());
    }

    @Test
    void blocksASecondActiveReservationOnTheSameScreening() throws Exception {
        var screeningId = materializedScreening(future(2));
        var bearer = verifiedCustomerBearer();
        var standards = seatsOfType(SeatType.STANDARD, 2);
        assertThat(reserve(bearer, screeningId, full(standards.get(0))).statusCode())
                .isEqualTo(201);

        var second = reserve(bearer, screeningId, full(standards.get(1)));

        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(second.body()).contains("\"code\":\"booking.active-reservation-exists\"");
    }

    @Test
    void companionSeatRequiresAnAccessibleSeat() throws Exception {
        var screeningId = materializedScreening(future(2));
        var bearer = verifiedCustomerBearer();
        var companion = seatOfType(SeatType.COMPANION);
        var accessible = seatOfType(SeatType.ACCESSIBLE);

        var rejected = reserve(bearer, screeningId, full(companion));
        assertThat(rejected.statusCode()).isEqualTo(409);
        assertThat(rejected.body()).contains("\"code\":\"booking.companion-requires-accessible\"");

        var accepted = reserve(bearer, screeningId, full(companion) + "," + full(accessible));
        assertThat(accepted.statusCode()).isEqualTo(201);
    }

    @Test
    void rejectsUnverifiedCustomer() throws Exception {
        var screeningId = materializedScreening(future(2));
        var bearer = customerBearer(false);

        var response = reserve(bearer, screeningId, full(seatOfType(SeatType.STANDARD)));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"user.email-not-verified\"");
    }

    @Test
    void rejectsReservationTooCloseToStart() throws Exception {
        var screeningId = materializedScreening(Instant.now().plus(Duration.ofMinutes(5)));

        var response = reserve(verifiedCustomerBearer(), screeningId, full(seatOfType(SeatType.STANDARD)));

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("\"code\":\"booking.screening-too-soon\"");
    }

    @Test
    void operatorsMayNotReserve() throws Exception {
        var screeningId = materializedScreening(future(2));
        var operator = staffBearer(Role.OPERATOR);

        assertThat(reserve(operator, screeningId, full(seatOfType(SeatType.STANDARD)))
                        .statusCode())
                .isEqualTo(403);
    }

    @Test
    void readingAReservationIsOwnerOrStaffOnly() throws Exception {
        var screeningId = materializedScreening(future(2));
        var ownerEmail = uniqueEmail();
        var owner = verifiedCustomerBearer(ownerEmail);
        var created = reserve(owner, screeningId, full(seatOfType(SeatType.STANDARD)));
        var reservationId = reservationIdOf(created);

        assertThat(get("/api/reservations/" + reservationId, "Authorization", owner)
                        .statusCode())
                .isEqualTo(200);
        assertThat(get("/api/reservations/" + reservationId, "Authorization", verifiedCustomerBearer())
                        .statusCode())
                .isEqualTo(403);
        assertThat(get("/api/reservations/" + reservationId, "Authorization", staffBearer(Role.ADMIN))
                        .statusCode())
                .isEqualTo(200);
        assertThat(get("/api/reservations/" + UUID.randomUUID(), "Authorization", owner)
                        .statusCode())
                .isEqualTo(404);
    }

    // --- helpers ---

    private UUID materializedScreening(Instant startsAt) {
        var movieId = movies.save(new Movie("Reservation Movie", 120, AgeRating.A14, null, null))
                .id();
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        var screeningId = screenings
                .save(Screening.schedule(movieId, roomId, startsAt, 120, 30, 3000))
                .id();
        materializer.materialize(screeningId, roomId);
        return screeningId;
    }

    private UUID seatOfType(SeatType type) {
        return seatsOfType(type, 1).get(0);
    }

    private List<UUID> seatsOfType(SeatType type, int count) {
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        return seats.findByRoomId(roomId).stream()
                .filter(seat -> seat.type() == type)
                .limit(count)
                .map(seat -> seat.id())
                .toList();
    }

    private HttpResponse<String> reserve(String bearer, UUID screeningId, String seatsJson)
            throws IOException, InterruptedException {
        return postJson(
                "/api/screenings/" + screeningId + "/reservations",
                "{\"seats\":[" + seatsJson + "]}",
                "Authorization",
                bearer);
    }

    private String full(UUID seatId) {
        return "{\"seatId\":\"%s\",\"ticketType\":\"FULL\"}".formatted(seatId);
    }

    private String half(UUID seatId) {
        return "{\"seatId\":\"%s\",\"ticketType\":\"HALF\",\"halfPriceCategory\":\"STUDENT\",\"documentReference\":\"UNE 1\"}"
                .formatted(seatId);
    }

    private String verifiedCustomerBearer() throws Exception {
        return verifiedCustomerBearer(uniqueEmail());
    }

    private String verifiedCustomerBearer(String email) throws Exception {
        return customerBearer(email, true);
    }

    private String customerBearer(boolean verified) throws Exception {
        return customerBearer(uniqueEmail(), verified);
    }

    private String customerBearer(String email, boolean verified) throws Exception {
        var user = User.newCustomer(email, passwordEncoder.encode(PASSWORD), "Customer", "pt-BR");
        if (verified) {
            user.verifyEmail(Instant.now());
        }
        users.save(user);
        return login(email);
    }

    private String staffBearer(Role role) throws Exception {
        var email = uniqueEmail();
        var user = new User(email, passwordEncoder.encode(PASSWORD), "Staff", role);
        user.verifyEmail(Instant.now());
        users.save(user);
        return login(email);
    }

    private String login(String email) throws Exception {
        return "Bearer "
                + accessTokenOf(postJson(
                        "/api/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)));
    }

    private static Instant future(int hours) {
        return Instant.now().plus(Duration.ofHours(hours));
    }

    private String reservationIdOf(HttpResponse<String> response) {
        var matcher = RESERVATION_ID.matcher(response.body());
        if (!matcher.find()) {
            throw new AssertionError("No reservationId in body: " + response.body());
        }
        return matcher.group(1);
    }
}
