package com.fksoft.application.screening;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.application.auth.RegistrationIntegrationTestSupport;
import com.fksoft.application.booking.ScreeningSeatRepository;
import com.fksoft.application.cinema.CinemaRoomRepository;
import com.fksoft.application.cinema.SeatRepository;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SPEC-0009 acceptance: schedule a screening, materialize its seats, enforce overlap and access. */
class ScreeningAdminIntegrationTest extends RegistrationIntegrationTestSupport {

    private static final Pattern ID = Pattern.compile("\"id\":\"([^\"]+)\"");

    @Autowired
    private MovieRepository movies;

    @Autowired
    private ScreeningRepository screenings;

    @Autowired
    private ScreeningSeatRepository screeningSeats;

    @Autowired
    private CinemaRoomRepository rooms;

    @Autowired
    private SeatRepository seats;

    @AfterEach
    void cleanScreeningData() {
        screeningSeats.deleteAll();
        screenings.deleteAll();
        movies.deleteAll();
    }

    @Test
    void createsScreeningAndMaterializesSeats() throws Exception {
        var bearer = adminBearer();
        var movieId = seedActiveMovie(120);
        var roomId = roomId("Room 1");

        var created = createScreening(bearer, movieId, roomId, future(2), 3000);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("\"status\":\"SCHEDULED\"").contains("\"endsAt\":\"");

        var screeningId = UUID.fromString(idOf(created));
        assertThat(screeningSeats.countByScreeningId(screeningId)).isEqualTo(seats.countByRoomId(roomId));
        assertThat(seats.countByRoomId(roomId)).isEqualTo(80);
    }

    @Test
    void rejectsOverlappingScreeningInSameRoom() throws Exception {
        var bearer = adminBearer();
        var movieId = seedActiveMovie(120);
        var roomId = roomId("Room 1");
        var start = future(3);

        assertThat(createScreening(bearer, movieId, roomId, start, 3000).statusCode())
                .isEqualTo(201);

        var overlap = createScreening(bearer, movieId, roomId, start.plus(Duration.ofMinutes(30)), 3000);
        assertThat(overlap.statusCode()).isEqualTo(409);
        assertThat(overlap.body()).contains("\"code\":\"screening.room-overlap\"");
    }

    @Test
    void rejectsScreeningStartingTooSoon() throws Exception {
        var bearer = adminBearer();
        var movieId = seedActiveMovie(120);

        var tooSoon = createScreening(bearer, movieId, roomId("Room 1"), future(0, 30), 3000);

        assertThat(tooSoon.statusCode()).isEqualTo(400);
        assertThat(tooSoon.body()).contains("\"code\":\"screening.starts-too-soon\"");
    }

    @Test
    void rejectsUnknownMovieOrRoom() throws Exception {
        var bearer = adminBearer();
        var movieId = seedActiveMovie(120);

        var badMovie = createScreening(bearer, UUID.randomUUID(), roomId("Room 1"), future(2), 3000);
        assertThat(badMovie.statusCode()).isEqualTo(404);
        assertThat(badMovie.body()).contains("\"code\":\"screening.movie-not-found\"");

        var badRoom = createScreening(bearer, movieId, UUID.randomUUID(), future(2), 3000);
        assertThat(badRoom.statusCode()).isEqualTo(404);
        assertThat(badRoom.body()).contains("\"code\":\"screening.room-not-found\"");
    }

    @Test
    void listsByRoomThenUpdatesAndCancels() throws Exception {
        var bearer = adminBearer();
        var movieId = seedActiveMovie(120);
        var room1 = roomId("Room 1");
        createScreening(bearer, movieId, room1, future(2), 3000);
        createScreening(bearer, movieId, roomId("Room 2"), future(2), 3000);

        var list = get("/api/admin/screenings?roomId=" + room1, "Authorization", bearer);
        assertThat(list.body()).contains("\"totalElements\":1");
        var id = idOf(list);

        var update = putJson(
                "/api/admin/screenings/" + id,
                "{\"movieId\":\"%s\",\"roomId\":\"%s\",\"startsAt\":\"%s\",\"basePriceCents\":5000}"
                        .formatted(movieId, room1, future(5)),
                "Authorization",
                bearer);
        assertThat(update.statusCode()).isEqualTo(200);
        assertThat(update.body()).contains("\"basePriceCents\":5000");

        var cancel = postJson("/api/admin/screenings/" + id + "/cancel", "", "Authorization", bearer);
        assertThat(cancel.statusCode()).isEqualTo(200);
        assertThat(cancel.body()).contains("\"status\":\"CANCELLED\"");
    }

    @Test
    void deletingAMovieWithScreeningsIsRejected() throws Exception {
        var bearer = adminBearer();
        var movieId = seedActiveMovie(120);
        createScreening(bearer, movieId, roomId("Room 1"), future(2), 3000);

        var blocked = delete("/api/admin/movies/" + movieId, "Authorization", bearer);
        assertThat(blocked.statusCode()).isEqualTo(409);
        assertThat(blocked.body()).contains("\"code\":\"movie.has-screenings\"");

        var freeMovie = seedActiveMovie(90);
        assertThat(delete("/api/admin/movies/" + freeMovie, "Authorization", bearer)
                        .statusCode())
                .isEqualTo(204);
    }

    @Test
    void nonAdminIsForbiddenAndAnonymousIsUnauthorized() throws Exception {
        var customerEmail = uniqueEmail();
        register(customerEmail);
        var customerBearer = "Bearer "
                + accessTokenOf(postJson(
                        "/api/auth/login",
                        "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(customerEmail, PASSWORD)));

        assertThat(get("/api/admin/screenings", "Authorization", customerBearer).statusCode())
                .isEqualTo(403);
        assertThat(get("/api/admin/screenings").statusCode()).isEqualTo(401);
    }

    private UUID seedActiveMovie(int durationMinutes) {
        var movie = new Movie("Test Movie", durationMinutes, AgeRating.A14, "synopsis", null);
        return movies.save(movie).id();
    }

    private UUID roomId(String name) {
        return rooms.findByName(name).orElseThrow().id();
    }

    private static Instant future(int hours) {
        return Instant.now().plus(Duration.ofHours(hours)).plusSeconds(60);
    }

    private static Instant future(int hours, int minutes) {
        return Instant.now().plus(Duration.ofHours(hours)).plus(Duration.ofMinutes(minutes));
    }

    private HttpResponse<String> createScreening(String bearer, UUID movieId, UUID roomId, Instant startsAt, int price)
            throws IOException, InterruptedException {
        var body = "{\"movieId\":\"%s\",\"roomId\":\"%s\",\"startsAt\":\"%s\",\"basePriceCents\":%d}"
                .formatted(movieId, roomId, startsAt, price);
        return postJson("/api/admin/screenings", body, "Authorization", bearer);
    }

    private String adminBearer() throws IOException, InterruptedException {
        var email = uniqueEmail();
        seedAdmin(email);
        return bearerFor(email);
    }

    private String idOf(HttpResponse<String> response) {
        var matcher = ID.matcher(response.body());
        if (!matcher.find()) {
            throw new AssertionError("No id in body: " + response.body());
        }
        return matcher.group(1);
    }
}
