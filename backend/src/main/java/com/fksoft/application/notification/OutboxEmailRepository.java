package com.fksoft.application.notification;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface OutboxEmailRepository extends JpaRepository<OutboxEmail, UUID> {

    /**
     * Claims due PENDING emails, skipping rows another worker already locked. SKIP LOCKED
     * keeps a slow send from blocking the next poll even though the deployment is
     * single-instance (ADR 0002).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select o from OutboxEmail o where o.status = com.fksoft.application.notification.OutboxStatus.PENDING "
            + "and o.nextAttemptAt <= :now order by o.nextAttemptAt asc")
    List<OutboxEmail> claimDue(@Param("now") Instant now, Pageable pageable);

    long countByStatus(OutboxStatus status);
}
