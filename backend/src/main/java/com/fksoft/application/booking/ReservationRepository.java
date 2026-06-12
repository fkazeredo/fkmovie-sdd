package com.fksoft.application.booking;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Reservation} (SPEC-0014). Module-internal. */
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    boolean existsByUserIdAndScreeningIdAndStatusIn(
            UUID userId, UUID screeningId, Collection<ReservationStatus> statuses);
}
