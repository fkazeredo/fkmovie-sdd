package com.fksoft.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Single-use invitation token (SPEC-0005). Only the SHA-256 hash is stored; valid for 24h
 * and consumed when the invitee accepts.
 */
@Entity
@Table(name = "invitation_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationToken {

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

    /** Created unconsumed; only the SHA-256 hash of the raw token is persisted. */
    public InvitationToken(UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
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
}
