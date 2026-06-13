package com.fksoft.application.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.application.auth.RegistrationIntegrationTestSupport;
import com.fksoft.application.auth.User;
import com.fksoft.application.cinema.CinemaRoomRepository;
import com.fksoft.application.payment.Payment;
import com.fksoft.application.payment.PaymentRepository;
import com.fksoft.application.pricing.TicketType;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SPEC-0019: owner-scoped reservation history list + enriched reservation detail. */
class MyReservationsIntegrationTest extends RegistrationIntegrationTestSupport {

    private static final AtomicInteger TICKET_SEQ = new AtomicInteger();

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
    private PaymentRepository payments;

    private final AtomicInteger daySlot = new AtomicInteger();

    @AfterEach
    void clean() {
        tickets.deleteAll();
        reservations.deleteAll();
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
        payments.deleteAll();
    }

    @Test
    void listsOnlyOwnReservations() throws Exception {
        var owner = verifiedCustomer();
        seedReservation(owner.id(), futureScreening(), ReservationStatus.PENDING);
        seedReservation(owner.id(), futureScreening(), ReservationStatus.CONFIRMED);
        var otherReservation = seedReservation(verifiedCustomer().id(), futureScreening(), ReservationStatus.PENDING);

        var body = list(bearerFor(owner.email()), "").body();

        assertThat(body).contains("\"totalElements\":2");
        assertThat(body).doesNotContain(otherReservation.toString());
        assertThat(body).contains("\"movieTitle\":\"My Movie\"").contains("\"roomName\":\"Room 1\"");
    }

    @Test
    void filtersByStatus() throws Exception {
        var owner = verifiedCustomer();
        seedReservation(owner.id(), futureScreening(), ReservationStatus.PENDING);
        var confirmed = seedReservation(owner.id(), futureScreening(), ReservationStatus.CONFIRMED);

        var body = list(bearerFor(owner.email()), "?status=CONFIRMED").body();

        assertThat(body).contains("\"totalElements\":1").contains(confirmed.toString());
    }

    @Test
    void clampsPageSizeToFifty() throws Exception {
        var owner = verifiedCustomer();
        seedReservation(owner.id(), futureScreening(), ReservationStatus.PENDING);

        var body = list(bearerFor(owner.email()), "?size=999").body();

        assertThat(body).contains("\"size\":50");
    }

    @Test
    void filtersUpcoming() throws Exception {
        var owner = verifiedCustomer();
        var upcoming = seedReservation(owner.id(), futureScreening(), ReservationStatus.CONFIRMED);
        var past = seedReservation(owner.id(), pastScreening(), ReservationStatus.CONFIRMED);

        var body = list(bearerFor(owner.email()), "?upcoming=true").body();

        assertThat(body)
                .contains("\"totalElements\":1")
                .contains(upcoming.toString())
                .doesNotContain(past.toString());
    }

    @Test
    void detailIsEnrichedForConfirmedReservation() throws Exception {
        var owner = verifiedCustomer();
        var reservationId = seedReservation(owner.id(), futureScreening(), ReservationStatus.CONFIRMED);

        var body = detail(bearerFor(owner.email()), reservationId).body();

        assertThat(body)
                .contains("\"movieTitle\":\"My Movie\"")
                .contains("\"roomName\":\"Room 1\"")
                .contains("\"startsAt\"")
                .containsPattern("\"code\":\"FKM-\\d{4}-\\d{6}\"")
                .contains("\"status\":\"CONFIRMED\"");
    }

    @Test
    void detailShowsRefundSummaryWhenRefunded() throws Exception {
        var owner = verifiedCustomer();
        var reservationId = seedReservation(owner.id(), futureScreening(), ReservationStatus.CONFIRMED);
        payments.save(Payment.refund(reservationId, 3000, Instant.now()));
        var reservation = reservations.findById(reservationId).orElseThrow();
        reservation.cancel();
        reservations.save(reservation);

        var body = detail(bearerFor(owner.email()), reservationId).body();

        assertThat(body).contains("\"refund\":{").contains("\"amountCents\":3000");
    }

    @Test
    void detailIsOwnerOrStaffScoped() throws Exception {
        var owner = verifiedCustomer();
        var reservationId = seedReservation(owner.id(), futureScreening(), ReservationStatus.PENDING);

        assertThat(detail(bearerFor(verifiedCustomer().email()), reservationId).statusCode())
                .isEqualTo(403);
        assertThat(detail(bearerFor(seedAdmin(uniqueEmail()).email()), reservationId)
                        .statusCode())
                .isEqualTo(200);
    }

    // --- helpers ---

    private HttpResponse<String> list(String bearer, String query) throws IOException, InterruptedException {
        return get("/api/me/reservations" + query, "Authorization", bearer);
    }

    private HttpResponse<String> detail(String bearer, UUID reservationId) throws IOException, InterruptedException {
        return get("/api/reservations/" + reservationId, "Authorization", bearer);
    }

    private UUID futureScreening() {
        return screening(Instant.now().plus(Duration.ofDays(1 + daySlot.getAndIncrement())));
    }

    private UUID pastScreening() {
        return screening(Instant.now().minus(Duration.ofDays(2)));
    }

    private UUID screening(Instant startsAt) {
        var movieId = movies.save(new Movie("My Movie", 120, AgeRating.A14, null, null))
                .id();
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        var screeningId = screenings
                .save(Screening.schedule(movieId, roomId, startsAt, 120, 30, 3000))
                .id();
        materializer.materialize(screeningId, roomId);
        return screeningId;
    }

    private UUID seedReservation(UUID userId, UUID screeningId, ReservationStatus status) {
        var seat = screeningSeats.findByScreeningId(screeningId).stream()
                .findFirst()
                .orElseThrow();
        seat.hold();
        if (status == ReservationStatus.CONFIRMED) {
            seat.sell();
        }
        screeningSeats.save(seat);
        var reservation = Reservation.pending(userId, screeningId, Instant.now().plus(Duration.ofHours(1)));
        reservation.addSeat(new ReservationSeat(seat.id(), TicketType.FULL, null, null, 3000));
        if (status == ReservationStatus.AWAITING_PAYMENT || status == ReservationStatus.CONFIRMED) {
            reservation.awaitPayment(UUID.randomUUID(), Instant.now().plus(Duration.ofMinutes(10)));
        }
        if (status == ReservationStatus.CONFIRMED) {
            reservation.confirm();
        }
        var saved = reservations.save(reservation);
        if (status == ReservationStatus.CONFIRMED) {
            var code = "FKM-2026-%06d".formatted(TICKET_SEQ.incrementAndGet());
            tickets.save(Ticket.issue(saved.seats().get(0).id(), code, Instant.now()));
        }
        return saved.id();
    }

    private User verifiedCustomer() {
        var user = User.newCustomer(uniqueEmail(), passwordEncoder.encode(PASSWORD), "Customer", "pt-BR");
        user.verifyEmail(Instant.now());
        return users.save(user);
    }
}
