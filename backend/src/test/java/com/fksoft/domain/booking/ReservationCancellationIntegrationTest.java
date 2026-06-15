package com.fksoft.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.domain.auth.RegistrationIntegrationTestSupport;
import com.fksoft.domain.auth.User;
import com.fksoft.domain.cinema.CinemaRoomRepository;
import com.fksoft.domain.cinema.SeatRepository;
import com.fksoft.domain.cinema.SeatType;
import com.fksoft.domain.payment.MockPaymentJobRepository;
import com.fksoft.domain.payment.PaymentKind;
import com.fksoft.domain.payment.PaymentRepository;
import com.fksoft.domain.pricing.TicketType;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.event.TransactionalEventListener;

/** SPEC-0018: the owner cancels; seats freed, tickets cancelled, refund requested for confirmed sales. */
@Import(ReservationCancellationIntegrationTest.CaptorConfig.class)
class ReservationCancellationIntegrationTest extends RegistrationIntegrationTestSupport {

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
    private CinemaRoomRepository rooms;

    @Autowired
    private SeatRepository seats;

    @Autowired
    private PaymentRepository payments;

    @Autowired
    private MockPaymentJobRepository jobs;

    @Autowired
    private EventCaptor captor;

    @AfterEach
    void clean() {
        tickets.deleteAll();
        reservations.deleteAll();
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
        jobs.deleteAll();
        payments.deleteAll();
        captor.seatChanges.clear();
        captor.statusChanges.clear();
    }

    @Test
    void cancelPendingReleasesSeatsWithoutRefund() throws Exception {
        var user = verifiedCustomer();
        var bearer = bearerFor(user.email());
        var screeningId = materializedScreening(Instant.now().plus(Duration.ofHours(2)));
        var seatId = standardSeat();
        var reservationId = reserveOneSeat(bearer, screeningId, seatId);

        var response = cancel(bearer, reservationId);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"CANCELLED\"").contains("\"refund\":{\"requested\":false");
        assertThat(screeningSeats.findByScreeningId(screeningId))
                .allMatch(seat -> seat.status() == ScreeningSeatStatus.FREE);
        assertThat(outboxEmails.findAll())
                .anyMatch(email -> email.recipientEmail().equals(user.email()));
        assertThat(captor.seatChanges).anyMatch(e -> e.status() == ScreeningSeatStatus.FREE);
        assertThat(captor.statusChanges).anyMatch(e -> e.status() == ReservationStatus.CANCELLED);
    }

    @Test
    void cancelAwaitingPaymentReleasesSeatsWithoutRefund() throws Exception {
        var user = verifiedCustomer();
        var bearer = bearerFor(user.email());
        var screeningId = materializedScreening(Instant.now().plus(Duration.ofHours(2)));
        var reservationId = reserveOneSeat(bearer, screeningId, standardSeat());
        assertThat(confirm(bearer, reservationId).statusCode()).isEqualTo(202);

        var response = cancel(bearer, reservationId);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"CANCELLED\"").contains("\"requested\":false");
        assertThat(screeningSeats.findByScreeningId(screeningId))
                .allMatch(seat -> seat.status() == ScreeningSeatStatus.FREE);
        assertThat(payments.findAll()).noneMatch(payment -> payment.kind() == PaymentKind.REFUND);
    }

    @Test
    void cancelConfirmedOutsideWindowRefundsReleasesSeatsAndCancelsTickets() throws Exception {
        var user = verifiedCustomer();
        var screeningId = materializedScreening(Instant.now().plus(Duration.ofDays(1)));
        var reservation = seedConfirmedReservation(user.id(), screeningId);

        var response = cancel(bearerFor(user.email()), reservation.id().toString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"status\":\"CANCELLED\"")
                .contains("\"refund\":{\"requested\":true,\"amountCents\":3000}");
        assertThat(screeningSeats.findByScreeningId(screeningId))
                .allMatch(seat -> seat.status() == ScreeningSeatStatus.FREE);
        assertThat(tickets.findAll()).allMatch(ticket -> ticket.status() == TicketStatus.CANCELLED);
        assertThat(payments.findAll()).anyMatch(payment -> payment.kind() == PaymentKind.REFUND);
    }

    @Test
    void cancelConfirmedInsideWindowIsRejected() throws Exception {
        var user = verifiedCustomer();
        var screeningId = materializedScreening(Instant.now().plus(Duration.ofMinutes(90)));
        var reservation = seedConfirmedReservation(user.id(), screeningId);

        var response = cancel(bearerFor(user.email()), reservation.id().toString());

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("\"code\":\"booking.cancellation-window-closed\"");
        assertThat(reservations.findById(reservation.id()).orElseThrow().status())
                .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(screeningSeats.findByScreeningId(screeningId).stream()
                        .filter(seat -> seat.status() == ScreeningSeatStatus.SOLD)
                        .count())
                .isEqualTo(1L); // the sold seat is untouched; nothing was released
    }

    @Test
    void onlyTheOwnerMayCancel() throws Exception {
        var owner = verifiedCustomer();
        var screeningId = materializedScreening(Instant.now().plus(Duration.ofHours(2)));
        var reservationId = reserveOneSeat(bearerFor(owner.email()), screeningId, standardSeat());

        var response = cancel(bearerFor(verifiedCustomer().email()), reservationId);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    // --- helpers ---

    private UUID materializedScreening(Instant startsAt) {
        var movieId = movies.save(new Movie("Cancel Movie", 120, AgeRating.A14, null, null))
                .id();
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        var screeningId = screenings
                .save(Screening.schedule(movieId, roomId, startsAt, 120, 30, 3000))
                .id();
        materializer.materialize(screeningId, roomId);
        return screeningId;
    }

    private UUID standardSeat() {
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        return seats.findByRoomId(roomId).stream()
                .filter(seat -> seat.type() == SeatType.STANDARD)
                .findFirst()
                .orElseThrow()
                .id();
    }

    private Reservation seedConfirmedReservation(UUID userId, UUID screeningId) {
        var seat = screeningSeats.findByScreeningId(screeningId).stream()
                .findFirst()
                .orElseThrow();
        seat.hold();
        seat.sell();
        screeningSeats.save(seat);
        var reservation = Reservation.pending(userId, screeningId, Instant.now().plus(Duration.ofHours(1)));
        reservation.addSeat(new ReservationSeat(seat.id(), TicketType.FULL, null, null, 3000));
        reservation.awaitPayment(UUID.randomUUID(), Instant.now().plus(Duration.ofMinutes(10)));
        reservation.confirm();
        var saved = reservations.save(reservation);
        var reservationSeatId = saved.seats().get(0).id();
        tickets.save(Ticket.issue(reservationSeatId, "FKM-2026-000777", Instant.now()));
        return saved;
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

    private HttpResponse<String> cancel(String bearer, String reservationId) throws IOException, InterruptedException {
        return postJson("/api/reservations/" + reservationId + "/cancel", "", "Authorization", bearer);
    }

    private User verifiedCustomer() {
        var user = User.newCustomer(uniqueEmail(), passwordEncoder.encode(PASSWORD), "Customer", "pt-BR");
        user.verifyEmail(Instant.now());
        return users.save(user);
    }

    @TestConfiguration
    static class CaptorConfig {
        @Bean
        EventCaptor cancellationEventCaptor() {
            return new EventCaptor();
        }
    }

    static class EventCaptor {
        private final List<SeatsStatusChanged> seatChanges = new CopyOnWriteArrayList<>();
        private final List<ReservationStatusChanged> statusChanges = new CopyOnWriteArrayList<>();

        @TransactionalEventListener
        void onSeats(SeatsStatusChanged event) {
            seatChanges.add(event);
        }

        @TransactionalEventListener
        void onStatus(ReservationStatusChanged event) {
            statusChanges.add(event);
        }
    }
}
