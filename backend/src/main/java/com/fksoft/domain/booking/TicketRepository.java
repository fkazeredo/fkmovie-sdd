package com.fksoft.domain.booking;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Persistence for {@link Ticket} (SPEC-0016). Module-internal. */
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByReservationSeatIdIn(Collection<UUID> reservationSeatIds);

    /** Exact lookup by the printed code — backs operator search (SPEC-0020). */
    Optional<Ticket> findByCode(String code);

    /** Next value of the global ticket-code sequence (no annual reset; uniqueness by sequence). */
    @Query(value = "select nextval('ticket_code_seq')", nativeQuery = true)
    long nextCodeValue();
}
