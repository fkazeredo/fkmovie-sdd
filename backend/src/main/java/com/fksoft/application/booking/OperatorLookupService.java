package com.fksoft.application.booking;

import com.fksoft.application.auth.UserAccounts;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operator (counter-staff) reservation lookup (SPEC-0020): search by exactly one of ticket code,
 * reservation id or customer email, and read a full reservation detail regardless of owner. The
 * operator only consults — no selling, no cancelling. Every search is audited (operator + criterion
 * type, never the raw email). Access is restricted to OPERATOR/ADMIN by the security chain.
 */
@Service
public class OperatorLookupService {

    private static final int EMAIL_MATCH_LIMIT = 200;
    private static final Logger log = LoggerFactory.getLogger(OperatorLookupService.class);

    private final ReservationRepository reservations;
    private final TicketRepository tickets;
    private final UserAccounts userAccounts;
    private final ReservationViewBuilder viewBuilder;

    OperatorLookupService(
            ReservationRepository reservations,
            TicketRepository tickets,
            UserAccounts userAccounts,
            ReservationViewBuilder viewBuilder) {
        this.reservations = reservations;
        this.tickets = tickets;
        this.userAccounts = userAccounts;
        this.viewBuilder = viewBuilder;
    }

    /** Searches reservations by exactly one criterion (SPEC-0020); emails are masked in the result. */
    @Transactional(readOnly = true)
    public List<OperatorReservationView> search(UUID operatorId, String ticketCode, UUID reservationId, String email) {
        var criterion = exactlyOne(ticketCode, reservationId, email);
        log.info("operator reservation search operatorId={} criterion={}", operatorId, criterion);
        var found =
                switch (criterion) {
                    case TICKET_CODE -> byTicketCode(ticketCode);
                    case RESERVATION_ID ->
                        reservations.findById(reservationId).map(List::of).orElseGet(List::of);
                    case EMAIL -> byEmail(email);
                };
        return found.stream().map(this::toOperatorView).toList();
    }

    /** Reads a full reservation detail with the customer's full contact (SPEC-0020); any owner. */
    @Transactional(readOnly = true)
    public OperatorReservationDetailView detail(UUID reservationId) {
        var reservation = reservations.findById(reservationId).orElseThrow(ReservationNotFoundException::new);
        var account = userAccounts.find(reservation.userId()).orElseThrow();
        return new OperatorReservationDetailView(viewBuilder.build(reservation), account.name(), account.email());
    }

    private List<Reservation> byTicketCode(String ticketCode) {
        return tickets.findByCode(ticketCode)
                .flatMap(ticket -> reservations.findBySeatId(ticket.reservationSeatId()))
                .map(List::of)
                .orElseGet(List::of);
    }

    private List<Reservation> byEmail(String email) {
        return userAccounts
                .findByEmail(email)
                .map(account -> reservations
                        .findByUserId(
                                account.userId(),
                                PageRequest.of(0, EMAIL_MATCH_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .getContent())
                .orElseGet(List::of);
    }

    private OperatorReservationView toOperatorView(Reservation reservation) {
        var view = viewBuilder.build(reservation);
        var account = userAccounts.find(reservation.userId()).orElse(null);
        return new OperatorReservationView(
                reservation.id(),
                reservation.status(),
                account == null ? "" : account.name(),
                account == null ? "" : maskEmail(account.email()),
                view.movieTitle(),
                view.startsAt(),
                view.seats().stream().map(seat -> seat.row() + seat.number()).toList());
    }

    private static Criterion exactlyOne(String ticketCode, UUID reservationId, String email) {
        var count = 0;
        Criterion present = null;
        if (ticketCode != null && !ticketCode.isBlank()) {
            count++;
            present = Criterion.TICKET_CODE;
        }
        if (reservationId != null) {
            count++;
            present = Criterion.RESERVATION_ID;
        }
        if (email != null && !email.isBlank()) {
            count++;
            present = Criterion.EMAIL;
        }
        if (count != 1) {
            throw new OperatorInvalidSearchException();
        }
        return present;
    }

    static String maskEmail(String email) {
        var at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private enum Criterion {
        TICKET_CODE,
        RESERVATION_ID,
        EMAIL
    }
}
