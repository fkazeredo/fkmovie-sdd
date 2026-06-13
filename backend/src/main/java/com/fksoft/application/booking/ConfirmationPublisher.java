package com.fksoft.application.booking;

import com.fksoft.application.auth.UserAccounts;
import com.fksoft.application.cinema.CinemaCatalog;
import com.fksoft.application.cinema.SeatView;
import com.fksoft.application.screening.ScreeningCatalog;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Emits the post-settlement events (SPEC-0016): on confirmation, the ticket email
 * ({@link ReservationConfirmed}, carrying the recipient + seat-labelled tickets) and the realtime
 * SOLD/CONFIRMED signals; on failure, the realtime FREE/CANCELLED signals. The STOMP send is 0013.
 */
@Component
class ConfirmationPublisher {

    private final UserAccounts userAccounts;
    private final CinemaCatalog cinema;
    private final ScreeningCatalog screenings;
    private final ScreeningSeatRepository screeningSeats;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ConfirmationPublisher(
            UserAccounts userAccounts,
            CinemaCatalog cinema,
            ScreeningCatalog screenings,
            ScreeningSeatRepository screeningSeats,
            ApplicationEventPublisher events,
            Clock clock) {
        this.userAccounts = userAccounts;
        this.cinema = cinema;
        this.screenings = screenings;
        this.screeningSeats = screeningSeats;
        this.events = events;
        this.clock = clock;
    }

    void publishConfirmed(Reservation reservation, List<Ticket> issuedTickets) {
        var account = userAccounts
                .find(reservation.userId())
                .orElseThrow(() -> new IllegalStateException("Account missing for reservation " + reservation.id()));
        var labels = seatLabelsByReservationSeatId(reservation);
        var ticketInfos = issuedTickets.stream()
                .map(ticket -> new ReservationConfirmed.TicketInfo(
                        ticket.code(), labels.getOrDefault(ticket.reservationSeatId(), "")))
                .toList();
        events.publishEvent(new ReservationConfirmed(
                reservation.id(),
                reservation.userId(),
                account.email(),
                account.name(),
                account.preferredLocale(),
                ticketInfos,
                clock.instant()));
        publishSeatStatus(reservation, ScreeningSeatStatus.SOLD);
        publishStatus(reservation, ReservationStatus.CONFIRMED);
    }

    void publishFailed(Reservation reservation) {
        publishSeatStatus(reservation, ScreeningSeatStatus.FREE);
        publishStatus(reservation, ReservationStatus.CANCELLED);
        events.publishEvent(new ReservationPaymentFailed(reservation.id(), reservation.userId(), clock.instant()));
    }

    private void publishSeatStatus(Reservation reservation, ScreeningSeatStatus status) {
        events.publishEvent(
                new SeatsStatusChanged(reservation.screeningId(), status, cinemaSeatIds(reservation), clock.instant()));
    }

    private void publishStatus(Reservation reservation, ReservationStatus status) {
        events.publishEvent(
                new ReservationStatusChanged(reservation.id(), reservation.userId(), status, clock.instant()));
    }

    private List<UUID> cinemaSeatIds(Reservation reservation) {
        return screeningSeats.findAllById(screeningSeatIds(reservation)).stream()
                .map(ScreeningSeat::seatId)
                .toList();
    }

    private Map<UUID, String> seatLabelsByReservationSeatId(Reservation reservation) {
        var screening = screenings.find(reservation.screeningId()).orElseThrow();
        var viewsBySeatId = cinema.seatsOf(screening.roomId()).stream()
                .collect(Collectors.toMap(SeatView::seatId, Function.identity()));
        var cinemaSeatByScreeningSeat = screeningSeats.findAllById(screeningSeatIds(reservation)).stream()
                .collect(Collectors.toMap(ScreeningSeat::id, ScreeningSeat::seatId));
        return reservation.seats().stream().collect(Collectors.toMap(ReservationSeat::id, seat -> {
            var view = viewsBySeatId.get(cinemaSeatByScreeningSeat.get(seat.screeningSeatId()));
            return view.row() + view.number();
        }));
    }

    private static List<UUID> screeningSeatIds(Reservation reservation) {
        return reservation.seats().stream()
                .map(ReservationSeat::screeningSeatId)
                .toList();
    }
}
