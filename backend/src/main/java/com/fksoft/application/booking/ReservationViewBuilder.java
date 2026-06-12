package com.fksoft.application.booking;

import com.fksoft.application.booking.ReservationView.ReservationSeatView;
import com.fksoft.application.cinema.CinemaCatalog;
import com.fksoft.application.cinema.SeatView;
import com.fksoft.application.screening.ScreeningCatalog;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Assembles the {@link ReservationView} read model (SPEC-0014): joins a reservation's seats
 * ({@code screening_seat_id} + snapshotted price) back to the cinema seat (row/number/type) via the
 * screening's room. Shared by the create response and {@code GET /api/reservations/{id}}.
 */
@Component
class ReservationViewBuilder {

    private final CinemaCatalog cinema;
    private final ScreeningCatalog screenings;
    private final ScreeningSeatRepository screeningSeats;

    ReservationViewBuilder(CinemaCatalog cinema, ScreeningCatalog screenings, ScreeningSeatRepository screeningSeats) {
        this.cinema = cinema;
        this.screenings = screenings;
        this.screeningSeats = screeningSeats;
    }

    ReservationView build(Reservation reservation) {
        var screening = screenings
                .find(reservation.screeningId())
                .orElseThrow(() -> new IllegalStateException("Screening missing for reservation " + reservation.id()));
        var seatIdByScreeningSeatId =
                screeningSeats
                        .findAllById(reservation.seats().stream()
                                .map(ReservationSeat::screeningSeatId)
                                .toList())
                        .stream()
                        .collect(Collectors.toMap(ScreeningSeat::id, ScreeningSeat::seatId));
        var viewsBySeatId = cinema.seatsOf(screening.roomId()).stream()
                .collect(Collectors.toMap(SeatView::seatId, Function.identity()));

        var seats = reservation.seats().stream()
                .map(seat -> {
                    var view = viewsBySeatId.get(seatIdByScreeningSeatId.get(seat.screeningSeatId()));
                    return new ReservationSeatView(
                            view.seatId(),
                            view.row(),
                            view.number(),
                            view.type(),
                            seat.ticketType(),
                            seat.priceCents());
                })
                .sorted(Comparator.comparing(ReservationSeatView::row).thenComparingInt(ReservationSeatView::number))
                .toList();
        return new ReservationView(
                reservation.id(),
                reservation.screeningId(),
                reservation.status(),
                reservation.expiresAt(),
                reservation.totalCents(),
                seats);
    }
}
