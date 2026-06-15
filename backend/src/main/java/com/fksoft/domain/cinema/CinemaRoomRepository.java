package com.fksoft.domain.cinema;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link CinemaRoom} (SPEC-0007). Module-internal. */
public interface CinemaRoomRepository extends JpaRepository<CinemaRoom, UUID> {

    Optional<CinemaRoom> findByName(String name);
}
