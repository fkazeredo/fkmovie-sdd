package com.fksoft.domain.auth;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("select la.attemptedAt from LoginAttempt la "
            + "where la.email = :email and la.succeeded = false and la.attemptedAt > :since "
            + "order by la.attemptedAt desc")
    List<Instant> recentFailureTimesByEmail(
            @Param("email") String email, @Param("since") Instant since, Pageable pageable);

    @Query("select la.attemptedAt from LoginAttempt la "
            + "where la.ip = :ip and la.succeeded = false and la.attemptedAt > :since "
            + "order by la.attemptedAt desc")
    List<Instant> recentFailureTimesByIp(@Param("ip") String ip, @Param("since") Instant since, Pageable pageable);
}
