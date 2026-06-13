package com.fksoft.application.booking;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/** Issues one VALID {@link Ticket} per reservation seat on confirmation (SPEC-0016). */
@Component
class TicketIssuer {

    private final TicketRepository tickets;
    private final TicketCodeGenerator codeGenerator;

    TicketIssuer(TicketRepository tickets, TicketCodeGenerator codeGenerator) {
        this.tickets = tickets;
        this.codeGenerator = codeGenerator;
    }

    List<Ticket> issue(Reservation reservation, Instant now) {
        return reservation.seats().stream()
                .map(seat -> tickets.save(Ticket.issue(seat.id(), codeGenerator.next(now), now)))
                .toList();
    }
}
