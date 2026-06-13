package com.fksoft.application.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.application.auth.RegistrationIntegrationTestSupport;
import com.fksoft.application.auth.User;
import com.fksoft.application.auth.UserRepository;
import com.fksoft.application.cinema.CinemaRoomRepository;
import com.fksoft.application.cinema.SeatRepository;
import com.fksoft.application.cinema.SeatType;
import com.fksoft.application.notification.OutboxEmailRepository;
import com.fksoft.application.notification.OutboxStatus;
import com.fksoft.application.payment.MockPaymentJobRepository;
import com.fksoft.application.payment.PaymentKind;
import com.fksoft.application.payment.PaymentRepository;
import com.fksoft.application.payment.PaymentWebhookEventRepository;
import com.fksoft.application.screening.AgeRating;
import com.fksoft.application.screening.Movie;
import com.fksoft.application.screening.MovieRepository;
import com.fksoft.application.screening.Screening;
import com.fksoft.application.screening.ScreeningRepository;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/** SPEC-0016 acceptance: confirm → mock pays via webhook → CONFIRMED, seats SOLD, tickets, email. */
class ConfirmationIntegrationTest extends RegistrationIntegrationTestSupport {

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
    private TicketRepository tickets;

    @Autowired
    private ReservationConfirmer confirmer;

    @Autowired
    private CinemaRoomRepository rooms;

    @Autowired
    private SeatRepository seats;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OutboxEmailRepository outbox;

    @Autowired
    private PaymentRepository payments;

    @Autowired
    private MockPaymentJobRepository jobs;

    @Autowired
    private PaymentWebhookEventRepository webhookEvents;

    @AfterEach
    void clean() {
        tickets.deleteAll();
        reservations.deleteAll();
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
        jobs.deleteAll();
        webhookEvents.deleteAll();
        payments.deleteAll();
        outbox.deleteAll();
    }

    @Test
    void confirmIssuesTicketsSellsSeatsAndEmails() throws Exception {
        var screeningId = materializedScreening(3000);
        var bearer = verifiedCustomerBearer();
        var seatId = seatOfType(SeatType.STANDARD);

        var reservationId = reserveOneSeat(bearer, screeningId, seatId);
        var confirm = confirm(bearer, reservationId);
        assertThat(confirm.statusCode()).isEqualTo(202);
        assertThat(confirm.body()).contains("\"status\":\"AWAITING_PAYMENT\"").contains("\"paymentId\":\"");

        await(() -> getReservation(bearer, reservationId).body().contains("\"status\":\"CONFIRMED\""));

        var view = getReservation(bearer, reservationId).body();
        assertThat(view).containsPattern("\"code\":\"FKM-\\d{4}-\\d{6}\"").contains("\"status\":\"VALID\"");
        assertThat(screeningSeats.findByScreeningId(screeningId).stream()
                        .filter(seat -> seat.status() == ScreeningSeatStatus.SOLD)
                        .count())
                .isEqualTo(1L);
        assertThat(outbox.countByStatus(OutboxStatus.PENDING)).isEqualTo(1);
    }

    @Test
    void failedPaymentCancelsAndReleasesSeats() throws Exception {
        // A total ending in …13 triggers the mock's deterministic failure path.
        var screeningId = materializedScreening(4513);
        var bearer = verifiedCustomerBearer();
        var reservationId = reserveOneSeat(bearer, screeningId, seatOfType(SeatType.STANDARD));

        assertThat(confirm(bearer, reservationId).statusCode()).isEqualTo(202);
        await(() -> getReservation(bearer, reservationId).body().contains("\"status\":\"CANCELLED\""));

        assertThat(screeningSeats.findByScreeningId(screeningId).stream())
                .allMatch(seat -> seat.status() == ScreeningSeatStatus.FREE);
    }

    @Test
    void onlyTheOwnerMayConfirm() throws Exception {
        var screeningId = materializedScreening(3000);
        var reservationId = reserveOneSeat(verifiedCustomerBearer(), screeningId, seatOfType(SeatType.STANDARD));

        var response = confirm(verifiedCustomerBearer(), reservationId);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void lateSuccessRequestsARefundWithoutChangingState() throws Exception {
        var screeningId = materializedScreening(3000);
        var bearer = verifiedCustomerBearer();
        var reservationId = reserveOneSeat(bearer, screeningId, seatOfType(SeatType.STANDARD));
        confirm(bearer, reservationId);
        await(() -> getReservation(bearer, reservationId).body().contains("\"status\":\"CONFIRMED\""));

        // A second (late) success for the already-CONFIRMED reservation: refund, no state change.
        confirmer.onPaymentSucceeded(UUID.fromString(reservationId), 3000);

        assertThat(reservations
                        .findById(UUID.fromString(reservationId))
                        .orElseThrow()
                        .status())
                .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(payments.findAll()).anyMatch(payment -> payment.kind() == PaymentKind.REFUND);
    }

    // --- helpers ---

    private UUID materializedScreening(int basePriceCents) {
        var movieId = movies.save(new Movie("Confirm Movie", 120, AgeRating.A14, null, null))
                .id();
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        var screeningId = screenings
                .save(Screening.schedule(
                        movieId, roomId, Instant.now().plus(Duration.ofHours(2)), 120, 30, basePriceCents))
                .id();
        materializer.materialize(screeningId, roomId);
        return screeningId;
    }

    private UUID seatOfType(SeatType type) {
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        return seats.findByRoomId(roomId).stream()
                .filter(seat -> seat.type() == type)
                .findFirst()
                .orElseThrow()
                .id();
    }

    private String reserveOneSeat(String bearer, UUID screeningId, UUID seatId) throws Exception {
        var response = postJson(
                "/api/screenings/" + screeningId + "/reservations",
                "{\"seats\":[{\"seatId\":\"%s\",\"ticketType\":\"FULL\"}]}".formatted(seatId),
                "Authorization",
                bearer);
        assertThat(response.statusCode()).isEqualTo(201);
        var matcher = RESERVATION_ID.matcher(response.body());
        matcher.find();
        return matcher.group(1);
    }

    private HttpResponse<String> confirm(String bearer, String reservationId) throws IOException, InterruptedException {
        return postJson("/api/reservations/" + reservationId + "/confirm", "", "Authorization", bearer);
    }

    private HttpResponse<String> getReservation(String bearer, String reservationId) {
        try {
            return get("/api/reservations/" + reservationId, "Authorization", bearer);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
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

    private void await(BooleanSupplier condition) {
        for (int i = 0; i < 100 && !condition.getAsBoolean(); i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        assertThat(condition.getAsBoolean()).as("condition met within timeout").isTrue();
    }
}
