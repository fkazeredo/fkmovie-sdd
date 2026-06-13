package com.fksoft.application.booking;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Reservation} (SPEC-0014). Module-internal. */
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    boolean existsByUserIdAndScreeningIdAndStatusIn(
            UUID userId, UUID screeningId, Collection<ReservationStatus> statuses);

    /**
     * Claims PENDING reservations past their hold expiry, skipping rows another sweep already locked
     * (SKIP LOCKED, lock timeout -2; SPEC-0017, ADR 0004). Backed by {@code idx_reservations_status_expires_at}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select r from Reservation r where r.status = com.fksoft.application.booking.ReservationStatus.PENDING "
            + "and r.expiresAt <= :now order by r.expiresAt asc")
    List<Reservation> claimExpired(@Param("now") Instant now, Pageable pageable);

    /**
     * Claims AWAITING_PAYMENT reservations past their payment deadline, skipping locked rows (SKIP
     * LOCKED; SPEC-0017). Backed by {@code idx_reservations_status_payment_deadline_at}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select r from Reservation r where r.status = "
            + "com.fksoft.application.booking.ReservationStatus.AWAITING_PAYMENT "
            + "and r.paymentDeadlineAt <= :now order by r.paymentDeadlineAt asc")
    List<Reservation> claimPaymentTimedOut(@Param("now") Instant now, Pageable pageable);
}
