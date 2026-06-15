package com.fksoft.domain.screening;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.AbstractIntegrationTest;
import com.fksoft.domain.cinema.CinemaRoomRepository;
import com.fksoft.domain.pricing.PriceCalculator;
import com.fksoft.domain.pricing.ScreeningPricingContext;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SPEC-0010: public, anonymous listing of upcoming SCHEDULED screenings of ACTIVE movies. */
class PublicScreeningsIntegrationTest extends AbstractIntegrationTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private MovieRepository movies;

    @Autowired
    private ScreeningRepository screenings;

    @Autowired
    private CinemaRoomRepository rooms;

    @Autowired
    private PriceCalculator pricing;

    private final AtomicInteger daySlot = new AtomicInteger();

    @AfterEach
    void clean() {
        screenings.deleteAll();
        movies.deleteAll();
    }

    @Test
    void listsOnlyFutureScheduledActive() throws Exception {
        var visible = futureScheduledOfActiveMovie();
        var archived = schedule(archivedMovie(), futureDay(), false);
        var past = schedule(activeMovie(), Instant.now().minus(Duration.ofDays(2)), false);
        var cancelled = schedule(activeMovie(), futureDay(), true);

        var body = get("/api/screenings").body();

        assertThat(body).contains(visible.toString());
        assertThat(body)
                .doesNotContain(archived.toString())
                .doesNotContain(past.toString())
                .doesNotContain(cancelled.toString());
    }

    @Test
    void filtersByCalendarDayInSaoPaulo() throws Exception {
        var day = LocalDate.now(SP).plusDays(30);
        var lateNight = day.atTime(23, 30).atZone(SP).toInstant(); // still "day" in SP, next day in UTC
        var screeningId = schedule(activeMovie(), lateNight, false);

        assertThat(get("/api/screenings?date=" + day).body()).contains(screeningId.toString());
        assertThat(get("/api/screenings?date=" + day.plusDays(1)).body()).doesNotContain(screeningId.toString());
    }

    @Test
    void exposesCheapestFullAsFromPrice() throws Exception {
        var startsAt = futureDay();
        var screeningId = schedule(activeMovie(), startsAt, false);
        var expected = pricing.cheapestFull(new ScreeningPricingContext(3000, startsAt));

        var body = get("/api/screenings").body();

        assertThat(body).contains(screeningId.toString()).contains("\"fromPriceCents\":" + expected);
    }

    @Test
    void doesNotSerializeEntityInternals() throws Exception {
        schedule(activeMovie(), futureDay(), false);

        var body = get("/api/screenings").body();

        assertThat(body)
                .doesNotContain("tenantId")
                .doesNotContain("basePriceCents")
                .doesNotContain("endsAt");
    }

    @Test
    void rejectsInvalidDate() throws Exception {
        var response = get("/api/screenings?date=not-a-date");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"screening.invalid-filter\"");
    }

    @Test
    void isPublicAndClampsPageSize() throws Exception {
        schedule(activeMovie(), futureDay(), false);

        var response = get("/api/screenings?size=999"); // no Authorization header

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"size\":100");
    }

    // --- helpers ---

    private UUID futureScheduledOfActiveMovie() {
        return schedule(activeMovie(), futureDay(), false);
    }

    private Instant futureDay() {
        return Instant.now().plus(Duration.ofDays(1 + daySlot.getAndIncrement()));
    }

    private UUID activeMovie() {
        return movies.save(new Movie("Public Movie", 117, AgeRating.A14, null, "https://poster"))
                .id();
    }

    private UUID archivedMovie() {
        var movie = new Movie("Archived Movie", 117, AgeRating.A14, null, null);
        movie.archive();
        return movies.save(movie).id();
    }

    private UUID schedule(UUID movieId, Instant startsAt, boolean cancelled) {
        var roomId = rooms.findByName("Room 1").orElseThrow().id();
        var screening = Screening.schedule(movieId, roomId, startsAt, 117, 30, 3000);
        if (cancelled) {
            screening.cancel();
        }
        return screenings.save(screening).id();
    }
}
