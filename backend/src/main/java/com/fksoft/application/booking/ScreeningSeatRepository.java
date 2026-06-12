package com.fksoft.application.booking;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link ScreeningSeat} (SPEC-0009/0011/0014). Module-internal. */
public interface ScreeningSeatRepository extends JpaRepository<ScreeningSeat, UUID> {

    boolean existsByScreeningId(UUID screeningId);

    long countByScreeningId(UUID screeningId);

    List<ScreeningSeat> findByScreeningId(UUID screeningId);

    /**
     * Loads the requested seats of a screening with a pessimistic write lock (SPEC-0014, ADR 0004):
     * {@code SELECT ... FOR UPDATE}. Ordered by id so concurrent reservations acquire locks in the
     * same order, avoiding deadlocks. Concurrent transactions on the same seat serialize here.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScreeningSeat s where s.screeningId = :screeningId and s.seatId in :seatIds order by s.id")
    List<ScreeningSeat> lockForReservation(
            @Param("screeningId") UUID screeningId, @Param("seatIds") Collection<UUID> seatIds);
}
