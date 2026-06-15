package com.fksoft.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Server-side record of an opaque refresh token (ADR 0005). Only the SHA-256 hash of the
 * token is stored; rotation marks the old row revoked and links it to its replacement so
 * reuse of a rotated token is detectable (SPEC-0003 token-theft rule).
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private UUID replacedById;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    protected RefreshToken() {
        // JPA
    }

    /** Created unrevoked; only the SHA-256 hash of the opaque token is ever persisted. */
    public RefreshToken(
            UUID userId, String tokenHash, Instant expiresAt, Instant createdAt, String ip, String userAgent) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    /** Rotation: this token dies and points to its successor for audit/reuse detection. */
    public void markReplacedBy(UUID successorId, Instant now) {
        revoke(now);
        this.replacedById = successorId;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public UUID replacedById() {
        return replacedById;
    }
}
