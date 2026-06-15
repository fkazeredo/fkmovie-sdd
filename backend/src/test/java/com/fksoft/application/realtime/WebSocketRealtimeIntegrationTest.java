package com.fksoft.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fksoft.domain.auth.RegistrationIntegrationTestSupport;
import com.fksoft.domain.auth.User;
import com.fksoft.domain.auth.UserRepository;
import com.fksoft.domain.booking.ReservationStatus;
import com.fksoft.domain.booking.ReservationStatusChanged;
import com.fksoft.domain.booking.ScreeningSeatStatus;
import com.fksoft.domain.booking.SeatsStatusChanged;
import com.fksoft.domain.cinema.CinemaRoomRepository;
import com.fksoft.domain.cinema.SeatRepository;
import com.fksoft.domain.cinema.SeatType;
import com.fksoft.domain.screening.AgeRating;
import com.fksoft.domain.screening.Movie;
import com.fksoft.domain.screening.MovieRepository;
import com.fksoft.domain.screening.Screening;
import com.fksoft.domain.screening.ScreeningRepository;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** SPEC-0013: STOMP CONNECT requires a valid JWT; seat/reservation changes are pushed after commit. */
class WebSocketRealtimeIntegrationTest extends RegistrationIntegrationTestSupport {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MovieRepository movies;

    @Autowired
    private ScreeningRepository screenings;

    @Autowired
    private CinemaRoomRepository rooms;

    @Autowired
    private SeatRepository seats;

    @Autowired
    private UserRepository users;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @AfterEach
    void clean() {
        screenings.deleteAll();
        movies.deleteAll();
    }

    @Test
    void deliversSeatUpdateToTheScreeningTopic() throws Exception {
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        var screeningId = screening(roomId);
        var seatId = seats.findByRoomId(roomId).stream()
                .filter(seat -> seat.type() == SeatType.STANDARD)
                .findFirst()
                .orElseThrow()
                .id();
        var session = connect(customerToken());
        var messages = subscribe(session, "/topic/screenings/" + screeningId + "/seats");

        publishInTransaction(
                new SeatsStatusChanged(screeningId, ScreeningSeatStatus.HELD, List.of(seatId), Instant.now()));

        var message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message.toString())
                .contains("SEAT_STATUS_CHANGED")
                .contains(seatId.toString())
                .contains("HELD");
        session.disconnect();
    }

    @Test
    void rejectsConnectWithoutValidToken() {
        assertThatThrownBy(() -> connect("not-a-valid-token"));
        assertThatThrownBy(() -> connect(null));
    }

    @Test
    void deliversReservationStatusToTheOwnerQueue() throws Exception {
        var userId = customerUserId();
        var session = connect(tokenFor(userId));
        var messages = subscribe(session, "/user/queue/reservations");

        publishInTransaction(
                new ReservationStatusChanged(UUID.randomUUID(), userId, ReservationStatus.CONFIRMED, Instant.now()));

        var message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message.toString()).contains("RESERVATION_STATUS_CHANGED").contains("CONFIRMED");
        session.disconnect();
    }

    // --- helpers ---

    private UUID screening(UUID roomId) {
        var movieId = movies.save(new Movie("WS Movie", 120, AgeRating.A14, null, null))
                .id();
        return screenings
                .save(Screening.schedule(movieId, roomId, Instant.now().plus(Duration.ofHours(2)), 120, 30, 3000))
                .id();
    }

    private StompSession connect(String token) throws Exception {
        var client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        var connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        var port = environment.getRequiredProperty("local.server.port", Integer.class);
        return client.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new org.springframework.web.socket.WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<Object> subscribe(StompSession session, String destination) throws InterruptedException {
        var queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add(payload);
            }
        });
        // Let the SUBSCRIBE register (esp. user-destination's extra resolver hop) before publishing.
        Thread.sleep(500);
        return queue;
    }

    private void publishInTransaction(Object event) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> events.publishEvent(event));
    }

    private String customerToken() throws Exception {
        return tokenFor(customerUserId());
    }

    private UUID customerUserId() {
        var email = uniqueEmail();
        var user = User.newCustomer(email, passwordEncoder.encode(PASSWORD), "Customer", "pt-BR");
        user.verifyEmail(Instant.now());
        return users.save(user).id();
    }

    private String tokenFor(UUID userId) throws Exception {
        var email = users.findById(userId).orElseThrow().email();
        return accessTokenOf(
                postJson("/api/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)));
    }
}
