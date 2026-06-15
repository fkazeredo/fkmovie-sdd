package com.fksoft.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.domain.auth.RegistrationIntegrationTestSupport;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/** SPEC-0017: the sweep expires stale holds, releases seats to FREE and emits the realtime events. */
@Import(ReservationExpirationIntegrationTest.CaptorConfig.class)
class ReservationExpirationIntegrationTest extends RegistrationIntegrationTestSupport {

    @Autowired
    private ReservationExpirationDispatcher dispatcher;

    @Autowired
    private ReservationRepository reservations;

    @Autowired
    private ScreeningSeatRepository screeningSeats;

    @Autowired
    private ScreeningSeatMaterializer materializer;

    @Autowired
    private MovieRepository movies;

    @Autowired
    private ScreeningRepository screenings;

    @Autowired
    private CinemaRoomRepository rooms;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EventCaptor captor;

    // Each seeded screening goes on a distinct day to avoid the room-overlap exclusion constraint (V9).
    private final AtomicInteger screeningSlot = new AtomicInteger();

    @AfterEach
    void clean() {
        reservations.deleteAll();
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
        captor.seatChanges.clear();
        captor.statusChanges.clear();
    }

    @Test
    void expiresPendingAndReleasesSeats() {
        var seat = seedHeldSeat();
        var reservationId = seedReservation(ReservationStatus.PENDING, past(), null, seat);

        dispatcher.sweep();

        assertThat(reservations.findById(reservationId).orElseThrow().status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(screeningSeats.findById(seat).orElseThrow().status()).isEqualTo(ScreeningSeatStatus.FREE);
        assertThat(captor.seatChanges).anyMatch(e -> e.status() == ScreeningSeatStatus.FREE);
        assertThat(captor.statusChanges).anyMatch(e -> e.status() == ReservationStatus.EXPIRED);
    }

    @Test
    void cancelsAwaitingPaymentPastDeadline() {
        var seat = seedHeldSeat();
        var reservationId = seedReservation(ReservationStatus.AWAITING_PAYMENT, future(), past(), seat);

        dispatcher.sweep();

        assertThat(reservations.findById(reservationId).orElseThrow().status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(screeningSeats.findById(seat).orElseThrow().status()).isEqualTo(ScreeningSeatStatus.FREE);
        assertThat(captor.statusChanges).anyMatch(e -> e.status() == ReservationStatus.CANCELLED);
    }

    @Test
    void leavesConfirmedUntouchedAndDoubleRunIsNoOp() {
        var soldSeat = seedSoldSeat();
        var confirmedId = seedReservation(ReservationStatus.CONFIRMED, past(), null, soldSeat);
        var heldSeat = seedHeldSeat();
        var pendingId = seedReservation(ReservationStatus.PENDING, past(), null, heldSeat);

        dispatcher.sweep();
        dispatcher.sweep(); // second pass must be a no-op, not an error

        assertThat(reservations.findById(confirmedId).orElseThrow().status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(screeningSeats.findById(soldSeat).orElseThrow().status()).isEqualTo(ScreeningSeatStatus.SOLD);
        assertThat(reservations.findById(pendingId).orElseThrow().status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(screeningSeats.findById(heldSeat).orElseThrow().status()).isEqualTo(ScreeningSeatStatus.FREE);
    }

    @Test
    @Timeout(20)
    void skipsRowsLockedByAnotherTransaction() throws Exception {
        var seat = seedHeldSeat();
        seedReservation(ReservationStatus.PENDING, past(), null, seat);

        var tx = new TransactionTemplate(transactionManager);
        var lockHeld = new CountDownLatch(1);
        var proceed = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        var locker = executor.submit(() -> tx.execute(status -> {
            var locked = reservations.claimExpired(Instant.now(), PageRequest.of(0, 100));
            lockHeld.countDown();
            awaitQuietly(proceed);
            return locked.size();
        }));

        assertThat(lockHeld.await(5, TimeUnit.SECONDS)).isTrue();
        var skipped = tx.execute(status -> reservations.claimExpired(Instant.now(), PageRequest.of(0, 100)));
        proceed.countDown();

        assertThat(skipped).isEmpty(); // SKIP LOCKED: the locked row is skipped, not blocked
        assertThat(locker.get(5, TimeUnit.SECONDS)).isEqualTo(1);
        executor.shutdownNow();
    }

    // --- helpers ---

    private Instant past() {
        return Instant.now().minus(Duration.ofMinutes(1));
    }

    private Instant future() {
        return Instant.now().plus(Duration.ofMinutes(30));
    }

    private UUID newScreening() {
        var movieId = movies.save(new Movie("Expire Movie", 120, AgeRating.A14, null, null))
                .id();
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        var startsAt = Instant.now().plus(Duration.ofDays(1 + screeningSlot.getAndIncrement()));
        var screeningId = screenings
                .save(Screening.schedule(movieId, roomId, startsAt, 120, 30, 3000))
                .id();
        materializer.materialize(screeningId, roomId);
        return screeningId;
    }

    private UUID seedHeldSeat() {
        var seat = firstSeatOf(newScreening());
        seat.hold();
        return screeningSeats.save(seat).id();
    }

    private UUID seedSoldSeat() {
        var seat = firstSeatOf(newScreening());
        seat.hold();
        seat.sell();
        return screeningSeats.save(seat).id();
    }

    private ScreeningSeat firstSeatOf(UUID screeningId) {
        return screeningSeats.findByScreeningId(screeningId).stream()
                .findFirst()
                .orElseThrow();
    }

    private UUID seedReservation(
            ReservationStatus status, Instant expiresAt, Instant paymentDeadlineAt, UUID screeningSeatId) {
        var screeningId = screeningSeats.findById(screeningSeatId).orElseThrow().screeningId();
        var reservation = Reservation.pending(newCustomerId(), screeningId, expiresAt);
        reservation.addSeat(new ReservationSeat(screeningSeatId, TicketType.FULL, null, null, 3000));
        switch (status) {
            case AWAITING_PAYMENT -> reservation.awaitPayment(UUID.randomUUID(), paymentDeadlineAt);
            case CONFIRMED -> reservation.confirm();
            default -> {
                /* PENDING as created */
            }
        }
        return reservations.save(reservation).id();
    }

    private UUID newCustomerId() {
        var user = User.newCustomer(uniqueEmail(), passwordEncoder.encode(PASSWORD), "Customer", "pt-BR");
        user.verifyEmail(Instant.now());
        return users.save(user).id();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @TestConfiguration
    static class CaptorConfig {
        @Bean
        EventCaptor reservationEventCaptor() {
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
