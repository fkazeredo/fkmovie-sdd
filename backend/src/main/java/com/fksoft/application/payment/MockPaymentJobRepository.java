package com.fksoft.application.payment;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

/** Persistence for the mock gateway's delivery queue (SPEC-0015). Module-internal. */
public interface MockPaymentJobRepository extends JpaRepository<MockPaymentJob, UUID> {

    /**
     * Claims due, undelivered jobs, skipping rows another poll already locked (SKIP LOCKED, lock
     * timeout -2). Mirrors the outbox dispatcher: safe even though the deployment is single-instance.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
            "select j from MockPaymentJob j where j.deliveredAt is null and j.deliverAt <= :now order by j.deliverAt asc")
    List<MockPaymentJob> claimDue(@Param("now") Instant now, Pageable pageable);
}
