package com.fksoft.domain.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Refresh token lifecycle (ADR 0005): opaque 256-bit tokens, stored only as SHA-256 hashes,
 * single-use rotation, and the token-theft rule — presenting a revoked token revokes every
 * token of that user (SPEC-0003).
 */
@Component
class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final Duration refreshTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    RefreshTokenService(RefreshTokenRepository repository, @Value("${app.jwt.refresh-ttl}") Duration refreshTtl) {
        this.repository = repository;
        this.refreshTtl = refreshTtl;
    }

    /** Issues a fresh token for the user; only the raw value ever leaves the server once. */
    IssuedRefreshToken issueFor(UUID userId, String ip, String userAgent, Instant now) {
        var raw = generateRawToken();
        var token = new RefreshToken(userId, sha256Hex(raw), now.plus(refreshTtl), now, ip, userAgent);
        repository.save(token);
        return new IssuedRefreshToken(raw, token);
    }

    /**
     * Single-use rotation: the presented token is revoked and linked to its successor.
     *
     * @throws InvalidRefreshTokenException when the token is unknown or expired.
     * @throws TokenReuseDetectedException when the token was already revoked — all of the
     *     user's tokens are revoked before throwing (suspected theft).
     */
    RotationResult rotate(String rawToken, String ip, String userAgent, Instant now) {
        var presented =
                repository.findForUpdateByTokenHash(sha256Hex(rawToken)).orElseThrow(InvalidRefreshTokenException::new);
        if (presented.isRevoked()) {
            repository.revokeAllForUser(presented.userId(), now);
            throw new TokenReuseDetectedException();
        }
        if (presented.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }
        var issued = issueFor(presented.userId(), ip, userAgent, now);
        presented.markReplacedBy(issued.token().id(), now);
        return new RotationResult(presented.userId(), issued);
    }

    /** Logout: revokes the presented token when it exists; unknown tokens are ignored. */
    java.util.Optional<UUID> revoke(String rawToken, Instant now) {
        return repository.findForUpdateByTokenHash(sha256Hex(rawToken)).map(token -> {
            token.revoke(now);
            return token.userId();
        });
    }

    void revokeAllForUser(UUID userId, Instant now) {
        repository.revokeAllForUser(userId, now);
    }

    static String sha256Hex(String raw) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String generateRawToken() {
        var bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    record IssuedRefreshToken(String rawToken, RefreshToken token) {}

    record RotationResult(UUID userId, IssuedRefreshToken issued) {}
}
