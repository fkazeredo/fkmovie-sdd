package com.fksoft.application.booking;

import com.fksoft.application.auth.UserAccounts;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Produces an audited, printable ticket reprint for an operator (SPEC-0020). Only VALID tickets of
 * CONFIRMED reservations are reprintable; the reprint records an audit row and emits an event but
 * never changes the ticket's state. The printable payload reuses the enriched reservation view.
 */
@Service
public class TicketReprintService {

    private static final Logger log = LoggerFactory.getLogger(TicketReprintService.class);

    private final TicketRepository tickets;
    private final ReservationRepository reservations;
    private final ReservationViewBuilder viewBuilder;
    private final UserAccounts userAccounts;
    private final TicketReprintRepository reprints;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;

    TicketReprintService(
            TicketRepository tickets,
            ReservationRepository reservations,
            ReservationViewBuilder viewBuilder,
            UserAccounts userAccounts,
            TicketReprintRepository reprints,
            ApplicationEventPublisher events,
            MeterRegistry meterRegistry) {
        this.tickets = tickets;
        this.reservations = reservations;
        this.viewBuilder = viewBuilder;
        this.userAccounts = userAccounts;
        this.reprints = reprints;
        this.events = events;
        this.meterRegistry = meterRegistry;
    }

    /** Records an audited reprint and returns the printable payload (SPEC-0020); no state change. */
    @Transactional
    public ReprintView reprint(UUID ticketId, UUID operatorId) {
        var ticket = tickets.findById(ticketId).orElseThrow(TicketNotFoundException::new);
        var reservation =
                reservations.findBySeatId(ticket.reservationSeatId()).orElseThrow(TicketNotFoundException::new);
        if (ticket.status() != TicketStatus.VALID || reservation.status() != ReservationStatus.CONFIRMED) {
            throw new TicketNotReprintableException();
        }
        var now = Instant.now();
        reprints.save(TicketReprint.record(ticketId, operatorId, now));
        events.publishEvent(new TicketReprinted(ticketId, operatorId, now));
        meterRegistry.counter("ticket_reprints_total").increment();
        log.info("ticket reprinted ticketId={} operatorId={}", ticketId, operatorId);

        var view = viewBuilder.build(reservation);
        var ticketView = view.tickets().stream()
                .filter(t -> t.ticketId().equals(ticketId))
                .findFirst()
                .orElseThrow(TicketNotFoundException::new);
        var account = userAccounts.find(reservation.userId()).orElseThrow();
        return new ReprintView(
                ticketView.code(),
                ticketView.seatLabel(),
                view.movieTitle(),
                view.roomName(),
                view.startsAt(),
                account.name(),
                reprints.countByTicketId(ticketId));
    }
}
