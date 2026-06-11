package com.fksoft.application.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Most recent token for a user — backs the forgot-password rate limit (SPEC-0004). */
    Optional<PasswordResetToken> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
