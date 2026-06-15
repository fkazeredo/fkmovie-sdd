package com.fksoft.domain.screening;

import com.fksoft.domain.cinema.CinemaCatalog;
import com.fksoft.domain.cinema.RoomView;
import com.fksoft.domain.pricing.PriceCalculator;
import com.fksoft.domain.pricing.ScreeningPricingContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public, anonymous listing of upcoming sessions (SPEC-0010): SCHEDULED screenings of ACTIVE movies in
 * the future, sorted by start, filterable by calendar day (America/Sao_Paulo) and movie. Each row
 * carries the movie/room display info and the "from" price (cheapest full price, pricing 0012).
 */
@Service
public class PublicScreeningsService {

    private static final ZoneId CINEMA_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ScreeningRepository screenings;
    private final MovieRepository movies;
    private final CinemaCatalog cinema;
    private final PriceCalculator pricing;
    private final MeterRegistry meterRegistry;

    PublicScreeningsService(
            ScreeningRepository screenings,
            MovieRepository movies,
            CinemaCatalog cinema,
            PriceCalculator pricing,
            MeterRegistry meterRegistry) {
        this.screenings = screenings;
        this.movies = movies;
        this.cinema = cinema;
        this.pricing = pricing;
        this.meterRegistry = meterRegistry;
    }

    /** Lists upcoming sessions (SPEC-0010); size clamped to 100, sorted by start ascending. */
    @Transactional(readOnly = true)
    public Page<PublicScreeningView> list(String date, UUID movieId, int page, int size) {
        var sample = Timer.start(meterRegistry);
        try {
            var now = Instant.now();
            var window = window(date, now);
            var pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.ASC, "startsAt"));
            var result = screenings.searchPublic(now, window.from(), window.to(), movieId, pageable);
            meterRegistry.counter("public_screenings_list_total").increment();
            return new PageImpl<>(assemble(result.getContent()), pageable, result.getTotalElements());
        } finally {
            sample.stop(Timer.builder("public_screenings_list_latency")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    private List<PublicScreeningView> assemble(List<Screening> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        var movieById = movies
                .findAllById(rows.stream().map(Screening::movieId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Movie::id, Function.identity()));
        var roomNameByRoom = rows.stream()
                .map(Screening::roomId)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), this::roomName));
        return rows.stream()
                .map(screening -> toView(screening, movieById.get(screening.movieId()), roomNameByRoom))
                .toList();
    }

    private PublicScreeningView toView(Screening screening, Movie movie, Map<UUID, String> roomNameByRoom) {
        var fromPrice =
                pricing.cheapestFull(new ScreeningPricingContext(screening.basePriceCents(), screening.startsAt()));
        return new PublicScreeningView(
                screening.id(),
                movie == null ? "" : movie.title(),
                movie == null ? null : movie.ageRating(),
                movie == null ? null : movie.posterUrl(),
                roomNameByRoom.getOrDefault(screening.roomId(), ""),
                screening.startsAt(),
                movie == null ? 0 : movie.durationMinutes(),
                fromPrice);
    }

    private String roomName(UUID roomId) {
        return cinema.findRoom(roomId).map(RoomView::name).orElse("");
    }

    private Window window(String date, Instant now) {
        if (date == null || date.isBlank()) {
            return new Window(now, now.plus(Duration.ofDays(36500)));
        }
        try {
            var day = LocalDate.parse(date.trim());
            return new Window(
                    day.atStartOfDay(CINEMA_ZONE).toInstant(),
                    day.plusDays(1).atStartOfDay(CINEMA_ZONE).toInstant());
        } catch (DateTimeParseException ex) {
            throw new ScreeningInvalidFilterException();
        }
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private record Window(Instant from, Instant to) {}
}
