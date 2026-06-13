package com.fksoft.application.booking;

import com.fksoft.application.booking.ReservationView.RefundSummary;
import com.fksoft.application.booking.ReservationView.ReservationSeatView;
import com.fksoft.application.booking.ReservationView.TicketView;
import com.fksoft.application.cinema.CinemaCatalog;
import com.fksoft.application.cinema.SeatView;
import com.fksoft.application.payment.PaymentLedger;
import com.fksoft.application.screening.MovieCatalog;
import com.fksoft.application.screening.ScreeningCatalog;
import com.fksoft.application.screening.ScreeningView;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Assembles the {@link ReservationView} read model (SPEC-0014/0016): joins a reservation's seats
 * ({@code screening_seat_id} + snapshotted price) back to the cinema seat (row/number/type) via the
 * screening's room, and the issued tickets once CONFIRMED. Shared by the create/confirm responses
 * and {@code GET /api/reservations/{id}}.
 */
@Component
class ReservationViewBuilder {

    private final CinemaCatalog cinema;
    private final ScreeningCatalog screenings;
    private final MovieCatalog movies;
    private final PaymentLedger paymentLedger;
    private final ScreeningSeatRepository screeningSeats;
    private final TicketRepository tickets;

    ReservationViewBuilder(
            CinemaCatalog cinema,
            ScreeningCatalog screenings,
            MovieCatalog movies,
            PaymentLedger paymentLedger,
            ScreeningSeatRepository screeningSeats,
            TicketRepository tickets) {
        this.cinema = cinema;
        this.screenings = screenings;
        this.movies = movies;
        this.paymentLedger = paymentLedger;
        this.screeningSeats = screeningSeats;
        this.tickets = tickets;
    }

    ReservationView build(Reservation reservation) {
        var screening = screenings
                .find(reservation.screeningId())
                .orElseThrow(() -> new IllegalStateException("Screening missing for reservation " + reservation.id()));
        var screeningSeatIds = reservation.seats().stream()
                .map(ReservationSeat::screeningSeatId)
                .toList();
        var seatIdByScreeningSeatId = screeningSeats.findAllById(screeningSeatIds).stream()
                .collect(Collectors.toMap(ScreeningSeat::id, ScreeningSeat::seatId));
        var viewsBySeatId = cinema.seatsOf(screening.roomId()).stream()
                .collect(Collectors.toMap(SeatView::seatId, Function.identity()));

        var seatViewByReservationSeatId = reservation.seats().stream()
                .collect(Collectors.toMap(
                        ReservationSeat::id,
                        seat -> viewsBySeatId.get(seatIdByScreeningSeatId.get(seat.screeningSeatId()))));
        var seats = reservation.seats().stream()
                .map(seat -> toSeatView(seat, seatViewByReservationSeatId.get(seat.id())))
                .sorted(Comparator.comparing(ReservationSeatView::row).thenComparingInt(ReservationSeatView::number))
                .toList();

        return new ReservationView(
                reservation.id(),
                reservation.screeningId(),
                reservation.status(),
                movieTitle(screening),
                roomName(screening.roomId()),
                screening.startsAt(),
                reservation.expiresAt(),
                reservation.paymentDeadlineAt(),
                reservation.totalCents(),
                seats,
                ticketViews(seatViewByReservationSeatId),
                refundSummary(reservation));
    }

    private String movieTitle(ScreeningView screening) {
        return movies.find(screening.movieId())
                .map(com.fksoft.application.screening.MovieView::title)
                .orElse("");
    }

    private String roomName(UUID roomId) {
        return cinema.findRoom(roomId)
                .map(com.fksoft.application.cinema.RoomView::name)
                .orElse("");
    }

    private RefundSummary refundSummary(Reservation reservation) {
        // A refund only exists for a cancelled-with-refund reservation (SPEC-0019); skip the query
        // otherwise — keeps it off the hot create/confirm path.
        if (reservation.status() != ReservationStatus.CANCELLED) {
            return null;
        }
        return paymentLedger
                .latestRefund(reservation.id())
                .map(refund ->
                        new RefundSummary(refund.amountCents(), refund.status().name()))
                .orElse(null);
    }

    private ReservationSeatView toSeatView(ReservationSeat seat, SeatView view) {
        return new ReservationSeatView(
                view.seatId(), view.row(), view.number(), view.type(), seat.ticketType(), seat.priceCents());
    }

    private List<TicketView> ticketViews(Map<UUID, SeatView> seatViewByReservationSeatId) {
        return tickets.findByReservationSeatIdIn(seatViewByReservationSeatId.keySet()).stream()
                .map(ticket -> {
                    var view = seatViewByReservationSeatId.get(ticket.reservationSeatId());
                    var label = view == null ? "" : view.row() + view.number();
                    return new TicketView(ticket.id(), ticket.code(), label, ticket.status());
                })
                .toList();
    }
}
