package com.fksoft.domain.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, UUID> {

    Optional<InvitationToken> findByTokenHash(String tokenHash);

    /** Most recent token for a user — backs the resend-invitation rate limit (SPEC-0005). */
    Optional<InvitationToken> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
