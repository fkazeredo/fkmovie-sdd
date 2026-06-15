package com.fksoft.domain.booking;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Issues one VALID {@link Ticket} per reservation seat on confirmation (SPEC-0016). */
@Component
@RequiredArgsConstructor
class TicketIssuer {

    private final TicketRepository tickets;
    private final TicketCodeGenerator codeGenerator;

    List<Ticket> issue(Reservation reservation, Instant now) {
        return reservation.seats().stream()
                .map(seat -> tickets.save(Ticket.issue(seat.id(), codeGenerator.next(now), now)))
                .toList();
    }
}
