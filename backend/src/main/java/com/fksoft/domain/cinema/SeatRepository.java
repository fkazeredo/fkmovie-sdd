package com.fksoft.domain.cinema;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Seat} (SPEC-0007). Module-internal. */
public interface SeatRepository extends JpaRepository<Seat, UUID> {

    long countByRoomId(UUID roomId);

    List<Seat> findByRoomId(UUID roomId);
}
