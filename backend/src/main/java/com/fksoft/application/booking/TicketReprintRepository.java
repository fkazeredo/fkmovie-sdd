package com.fksoft.application.booking;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link TicketReprint} (SPEC-0020). Module-internal. */
public interface TicketReprintRepository extends JpaRepository<TicketReprint, UUID> {

    long countByTicketId(UUID ticketId);
}
