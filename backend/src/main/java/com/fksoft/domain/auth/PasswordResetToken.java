package com.fksoft.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Single-use password-reset token (SPEC-0004). Only the SHA-256 hash is stored; valid for
 * 1h and consumed exactly once. Records the requesting IP for audit.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "requested_ip")
    private String requestedIp;

    protected PasswordResetToken() {
        // JPA
    }

    /** Created unconsumed; only the SHA-256 hash of the raw token is persisted. */
    public PasswordResetToken(UUID userId, String tokenHash, Instant expiresAt, Instant createdAt, String requestedIp) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.requestedIp = requestedIp;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public void consume(Instant now) {
        this.consumedAt = now;
    }

    public UUID userId() {
        return userId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
