package com.fksoft.application.booking;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ScreeningSeat} (SPEC-0009). Module-internal. */
public interface ScreeningSeatRepository extends JpaRepository<ScreeningSeat, UUID> {

    boolean existsByScreeningId(UUID screeningId);

    long countByScreeningId(UUID screeningId);
}
