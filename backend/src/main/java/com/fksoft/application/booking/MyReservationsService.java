package com.fksoft.application.booking;

import com.fksoft.application.cinema.CinemaCatalog;
import com.fksoft.application.cinema.SeatView;
import com.fksoft.application.screening.MovieCatalog;
import com.fksoft.application.screening.MovieView;
import com.fksoft.application.screening.ScreeningCatalog;
import com.fksoft.application.screening.ScreeningView;
import com.fksoft.shared.pagination.PageResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the authenticated customer's reservation history (SPEC-0019): owner-scoped, newest first,
 * optionally filtered by status and by upcoming sessions. Each row is enriched with the screening's
 * movie/room/start and the seat labels, assembled in batch through the cinema/screening read facades.
 */
@Service
public class MyReservationsService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ReservationRepository reservations;
    private final ScreeningSeatRepository screeningSeats;
    private final ScreeningCatalog screenings;
    private final MovieCatalog movies;
    private final CinemaCatalog cinema;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    MyReservationsService(
            ReservationRepository reservations,
            ScreeningSeatRepository screeningSeats,
            ScreeningCatalog screenings,
            MovieCatalog movies,
            CinemaCatalog cinema,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.reservations = reservations;
        this.screeningSeats = screeningSeats;
        this.screenings = screenings;
        this.movies = movies;
        this.cinema = cinema;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    /** Lists the caller's reservations (SPEC-0019); size clamped to 50, newest first. */
    @Transactional(readOnly = true)
    public PageResponse<MyReservationView> list(
            UUID callerId, ReservationStatus status, boolean upcoming, int page, int size) {
        var sample = Timer.start(meterRegistry);
        try {
            var pageable =
                    PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
            var result = query(callerId, status, upcoming, pageable);
            var items = assemble(result.getContent());
            return new PageResponse<>(
                    items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
        } finally {
            sample.stop(Timer.builder("my_reservations_list_latency")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    private Page<Reservation> query(UUID callerId, ReservationStatus status, boolean upcoming, Pageable pageable) {
        if (upcoming) {
            var futureIds = screenings.futureScreeningIds(clock.instant());
            if (futureIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return status == null
                    ? reservations.findByUserIdAndScreeningIdIn(callerId, futureIds, pageable)
                    : reservations.findByUserIdAndStatusAndScreeningIdIn(callerId, status, futureIds, pageable);
        }
        return status == null
                ? reservations.findByUserId(callerId, pageable)
                : reservations.findByUserIdAndStatus(callerId, status, pageable);
    }

    private List<MyReservationView> assemble(List<Reservation> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        var lookups = buildLookups(rows);
        return rows.stream().map(row -> toView(row, lookups)).toList();
    }

    private Lookups buildLookups(List<Reservation> rows) {
        var screeningById = screenings
                .findAll(rows.stream().map(Reservation::screeningId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ScreeningView::id, Function.identity()));
        var movieById =
                movies
                        .findAll(screeningById.values().stream()
                                .map(ScreeningView::movieId)
                                .distinct()
                                .toList())
                        .stream()
                        .collect(Collectors.toMap(MovieView::id, Function.identity()));
        var allScreeningSeatIds = rows.stream()
                .flatMap(row -> row.seats().stream().map(ReservationSeat::screeningSeatId))
                .distinct()
                .toList();
        var cinemaSeatByScreeningSeat = screeningSeats.findAllById(allScreeningSeatIds).stream()
                .collect(Collectors.toMap(ScreeningSeat::id, ScreeningSeat::seatId));
        var seatsByRoom = screeningById.values().stream()
                .map(ScreeningView::roomId)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), this::seatViewsOf));
        return new Lookups(screeningById, movieById, cinemaSeatByScreeningSeat, seatsByRoom);
    }

    private Map<UUID, SeatView> seatViewsOf(UUID roomId) {
        return cinema.seatsOf(roomId).stream().collect(Collectors.toMap(SeatView::seatId, Function.identity()));
    }

    private MyReservationView toView(Reservation reservation, Lookups lookups) {
        var screening = lookups.screeningById().get(reservation.screeningId());
        var movie = screening == null ? null : lookups.movieById().get(screening.movieId());
        var seatViews = screening == null
                ? Map.<UUID, SeatView>of()
                : lookups.seatsByRoom().getOrDefault(screening.roomId(), Map.of());
        var seatLabels = reservation.seats().stream()
                .map(seat -> seatViews.get(lookups.cinemaSeatByScreeningSeat().get(seat.screeningSeatId())))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SeatView::row).thenComparingInt(SeatView::number))
                .map(view -> view.row() + view.number())
                .toList();
        return new MyReservationView(
                reservation.id(),
                reservation.status(),
                movie == null ? "" : movie.title(),
                screening == null ? "" : roomName(screening.roomId()),
                screening == null ? null : screening.startsAt(),
                reservation.totalCents(),
                seatLabels);
    }

    private String roomName(UUID roomId) {
        return cinema.findRoom(roomId)
                .map(com.fksoft.application.cinema.RoomView::name)
                .orElse("");
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private record Lookups(
            Map<UUID, ScreeningView> screeningById,
            Map<UUID, MovieView> movieById,
            Map<UUID, UUID> cinemaSeatByScreeningSeat,
            Map<UUID, Map<UUID, SeatView>> seatsByRoom) {}
}
