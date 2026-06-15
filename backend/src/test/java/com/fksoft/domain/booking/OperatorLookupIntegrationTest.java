package com.fksoft.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.domain.auth.RegistrationIntegrationTestSupport;
import com.fksoft.domain.auth.Role;
import com.fksoft.domain.auth.User;
import com.fksoft.domain.cinema.CinemaRoomRepository;
import com.fksoft.domain.pricing.TicketType;
import com.fksoft.domain.screening.AgeRating;
import com.fksoft.domain.screening.Movie;
import com.fksoft.domain.screening.MovieRepository;
import com.fksoft.domain.screening.Screening;
import com.fksoft.domain.screening.ScreeningRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SPEC-0020: operator search by one criterion, full detail, and audited ticket reprint. */
class OperatorLookupIntegrationTest extends RegistrationIntegrationTestSupport {

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
    private TicketReprintRepository ticketReprints;

    @Autowired
    private CinemaRoomRepository rooms;

    private final AtomicInteger daySlot = new AtomicInteger();

    @AfterEach
    void clean() {
        ticketReprints.deleteAll();
        tickets.deleteAll();
        reservations.deleteAll();
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
    }

    @Test
    void searchesByEachCriterionAndMasksEmail() throws Exception {
        var seeded = seedConfirmed();
        var operator = operatorBearer();

        var byCode = get("/api/operator/reservations?ticketCode=" + seeded.ticketCode(), "Authorization", operator);
        assertThat(byCode.statusCode()).isEqualTo(200);
        assertThat(byCode.body())
                .contains(seeded.reservationId().toString())
                .contains("\"customerEmail\":\"u***@") // masked, not the full address
                .doesNotContain(seeded.customerEmail());

        assertThat(get("/api/operator/reservations?reservationId=" + seeded.reservationId(), "Authorization", operator)
                        .body())
                .contains(seeded.reservationId().toString());
        assertThat(get("/api/operator/reservations?email=" + seeded.customerEmail(), "Authorization", operator)
                        .body())
                .contains(seeded.reservationId().toString());
    }

    @Test
    void rejectsZeroOrMultipleCriteria() throws Exception {
        var operator = operatorBearer();

        assertThat(get("/api/operator/reservations", "Authorization", operator).statusCode())
                .isEqualTo(400);
        var multi = get("/api/operator/reservations?ticketCode=X&email=y@z.com", "Authorization", operator);
        assertThat(multi.statusCode()).isEqualTo(400);
        assertThat(multi.body()).contains("\"code\":\"operator.invalid-search\"");
    }

    @Test
    void customerTokenIsForbidden() throws Exception {
        var seeded = seedConfirmed();
        var customer = bearerFor(verifiedCustomer().email());

        var response =
                get("/api/operator/reservations?reservationId=" + seeded.reservationId(), "Authorization", customer);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void detailExposesFullEmail() throws Exception {
        var seeded = seedConfirmed();

        var body = get("/api/operator/reservations/" + seeded.reservationId(), "Authorization", operatorBearer())
                .body();

        assertThat(body).contains(seeded.customerEmail()).contains("\"movieTitle\":\"Op Movie\"");
    }

    @Test
    void reprintsValidTicketAndAudits() throws Exception {
        var seeded = seedConfirmed();

        var response = postJson(
                "/api/operator/tickets/" + seeded.ticketId() + "/reprint", "", "Authorization", operatorBearer());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"ticketCode\":\"" + seeded.ticketCode() + "\"")
                .contains("\"reprintCount\":1");
        assertThat(ticketReprints.countByTicketId(seeded.ticketId())).isEqualTo(1L);
        assertThat(tickets.findById(seeded.ticketId()).orElseThrow().status()).isEqualTo(TicketStatus.VALID);
    }

    @Test
    void doesNotReprintCancelledTicket() throws Exception {
        var seeded = seedConfirmed();
        var ticket = tickets.findById(seeded.ticketId()).orElseThrow();
        ticket.cancel();
        tickets.save(ticket);

        var response = postJson(
                "/api/operator/tickets/" + seeded.ticketId() + "/reprint", "", "Authorization", operatorBearer());

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("\"code\":\"booking.ticket-not-reprintable\"");
    }

    @Test
    void returns404ForUnknownTicket() throws Exception {
        var response = postJson(
                "/api/operator/tickets/" + UUID.randomUUID() + "/reprint", "", "Authorization", operatorBearer());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"code\":\"booking.ticket-not-found\"");
    }

    // --- helpers ---

    private String operatorBearer() throws Exception {
        var operator = new User(uniqueEmail(), passwordEncoder.encode(PASSWORD), "Operator", Role.OPERATOR);
        operator.verifyEmail(Instant.now());
        return bearerFor(users.save(operator).email());
    }

    private User verifiedCustomer() {
        var user = User.newCustomer(uniqueEmail(), passwordEncoder.encode(PASSWORD), "Customer", "pt-BR");
        user.verifyEmail(Instant.now());
        return users.save(user);
    }

    private Seeded seedConfirmed() {
        var customer = verifiedCustomer();
        var movieId = movies.save(new Movie("Op Movie", 120, AgeRating.A14, null, null))
                .id();
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        var startsAt = Instant.now().plus(Duration.ofDays(1 + daySlot.getAndIncrement()));
        var screeningId = screenings
                .save(Screening.schedule(movieId, roomId, startsAt, 120, 30, 3000))
                .id();
        materializer.materialize(screeningId, roomId);
        var seat = screeningSeats.findByScreeningId(screeningId).stream()
                .findFirst()
                .orElseThrow();
        seat.hold();
        seat.sell();
        screeningSeats.save(seat);
        var reservation =
                Reservation.pending(customer.id(), screeningId, Instant.now().plus(Duration.ofHours(1)));
        reservation.addSeat(new ReservationSeat(seat.id(), TicketType.FULL, null, null, 3000));
        reservation.awaitPayment(UUID.randomUUID(), Instant.now().plus(Duration.ofMinutes(10)));
        reservation.confirm();
        var saved = reservations.save(reservation);
        var code = "FKM-2026-%06d".formatted(TICKET_SEQ.incrementAndGet());
        var ticket = tickets.save(Ticket.issue(saved.seats().get(0).id(), code, Instant.now()));
        return new Seeded(customer.email(), saved.id(), ticket.id(), code);
    }

    private record Seeded(String customerEmail, UUID reservationId, UUID ticketId, String ticketCode) {}
}
