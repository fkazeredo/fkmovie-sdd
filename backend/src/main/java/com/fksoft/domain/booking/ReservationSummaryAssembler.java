package com.fksoft.domain.booking;

import com.fksoft.domain.cinema.CinemaCatalog;
import com.fksoft.domain.cinema.RoomView;
import com.fksoft.domain.cinema.SeatView;
import com.fksoft.domain.screening.MovieCatalog;
import com.fksoft.domain.screening.MovieView;
import com.fksoft.domain.screening.ScreeningCatalog;
import com.fksoft.domain.screening.ScreeningView;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Builds lightweight reservation summaries in batch (SPEC-0019/0020): for a page/result set, it
 * resolves each reservation's screening, movie title, room name and seat labels through the read
 * facades with per-set caching — avoiding the N+1 of building a full {@link ReservationView} per row.
 * Shared by the "my reservations" list and the operator search.
 */
@Component
class ReservationSummaryAssembler {

    private final ScreeningCatalog screenings;
    private final MovieCatalog movies;
    private final CinemaCatalog cinema;
    private final ScreeningSeatRepository screeningSeats;

    ReservationSummaryAssembler(
            ScreeningCatalog screenings,
            MovieCatalog movies,
            CinemaCatalog cinema,
            ScreeningSeatRepository screeningSeats) {
        this.screenings = screenings;
        this.movies = movies;
        this.cinema = cinema;
        this.screeningSeats = screeningSeats;
    }

    List<ReservationSummary> summarize(List<Reservation> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        var lookups = buildLookups(rows);
        return rows.stream().map(row -> toSummary(row, lookups)).toList();
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
        var cinemaSeatByScreeningSeat = screeningSeats
                .findAllById(rows.stream()
                        .flatMap(row -> row.seats().stream().map(ReservationSeat::screeningSeatId))
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(ScreeningSeat::id, ScreeningSeat::seatId));
        var seatsByRoom = screeningById.values().stream()
                .map(ScreeningView::roomId)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), this::seatViewsOf));
        var roomNameByRoom = screeningById.values().stream()
                .map(ScreeningView::roomId)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), this::roomName));
        return new Lookups(screeningById, movieById, cinemaSeatByScreeningSeat, seatsByRoom, roomNameByRoom);
    }

    private Map<UUID, SeatView> seatViewsOf(UUID roomId) {
        return cinema.seatsOf(roomId).stream().collect(Collectors.toMap(SeatView::seatId, Function.identity()));
    }

    private String roomName(UUID roomId) {
        return cinema.findRoom(roomId).map(RoomView::name).orElse("");
    }

    private ReservationSummary toSummary(Reservation reservation, Lookups lookups) {
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
        return new ReservationSummary(
                reservation.id(),
                reservation.status(),
                movie == null ? "" : movie.title(),
                screening == null ? "" : lookups.roomNameByRoom().getOrDefault(screening.roomId(), ""),
                screening == null ? null : screening.startsAt(),
                reservation.totalCents(),
                seatLabels);
    }

    private record Lookups(
            Map<UUID, ScreeningView> screeningById,
            Map<UUID, MovieView> movieById,
            Map<UUID, UUID> cinemaSeatByScreeningSeat,
            Map<UUID, Map<UUID, SeatView>> seatsByRoom,
            Map<UUID, String> roomNameByRoom) {}

    /** A lightweight reservation projection for list/search rows (SPEC-0019/0020). */
    record ReservationSummary(
            UUID reservationId,
            ReservationStatus status,
            String movieTitle,
            String roomName,
            Instant startsAt,
            int totalCents,
            List<String> seatLabels) {}
}
