package com.fksoft.domain.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Issues and consumes the single-use email-verification (24h), password-reset (1h) and
 * invitation (24h) tokens of SPEC-0004/0005. Tokens are 256-bit opaque values; only their
 * SHA-256 hash is stored (reusing {@link RefreshTokenService#sha256Hex}). Also enforces the
 * resend rate limits (verification/reset 1/min; invitation 1/5min).
 */
@Component
@RequiredArgsConstructor
class VerificationTokenService {

    static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    static final Duration RESET_TTL = Duration.ofHours(1);
    static final Duration INVITATION_TTL = Duration.ofHours(24);
    static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);
    static final Duration INVITATION_RESEND_INTERVAL = Duration.ofMinutes(5);

    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final InvitationTokenRepository invitationTokens;
    private final SecureRandom secureRandom = new SecureRandom();

    String issueEmailVerification(UUID userId, Instant now) {
        var raw = generateRawToken();
        verificationTokens.save(new EmailVerificationToken(
                userId, RefreshTokenService.sha256Hex(raw), now.plus(VERIFICATION_TTL), now));
        return raw;
    }

    /**
     * Validates and consumes an email-verification token.
     *
     * @throws TokenInvalidException when the token is unknown/malformed.
     * @throws TokenExpiredException when it is expired or already consumed.
     */
    UUID consumeEmailVerification(String rawToken, Instant now) {
        var token = verificationTokens
                .findByTokenHash(RefreshTokenService.sha256Hex(rawToken))
                .orElseThrow(TokenInvalidException::new);
        return consume(token.isConsumed(), token.isExpired(now), token::consume, token.userId(), now);
    }

    String issuePasswordReset(UUID userId, String ip, Instant now) {
        var raw = generateRawToken();
        resetTokens.save(
                new PasswordResetToken(userId, RefreshTokenService.sha256Hex(raw), now.plus(RESET_TTL), now, ip));
        return raw;
    }

    /** Validates and consumes a password-reset token (same failure semantics as verification). */
    UUID consumePasswordReset(String rawToken, Instant now) {
        var token = resetTokens
                .findByTokenHash(RefreshTokenService.sha256Hex(rawToken))
                .orElseThrow(TokenInvalidException::new);
        return consume(token.isConsumed(), token.isExpired(now), token::consume, token.userId(), now);
    }

    String issueInvitation(UUID userId, Instant now) {
        var raw = generateRawToken();
        invitationTokens.save(
                new InvitationToken(userId, RefreshTokenService.sha256Hex(raw), now.plus(INVITATION_TTL), now));
        return raw;
    }

    /** Validates and consumes an invitation token (same failure semantics as verification). */
    UUID consumeInvitation(String rawToken, Instant now) {
        var token = invitationTokens
                .findByTokenHash(RefreshTokenService.sha256Hex(rawToken))
                .orElseThrow(TokenInvalidException::new);
        return consume(token.isConsumed(), token.isExpired(now), token::consume, token.userId(), now);
    }

    void assertVerificationResendAllowed(UUID userId, Instant now) {
        verificationTokens
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .ifPresent(last -> requireOutsideWindow(last.createdAt(), now, RESEND_INTERVAL));
    }

    void assertResetRequestAllowed(UUID userId, Instant now) {
        resetTokens
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .ifPresent(last -> requireOutsideWindow(last.createdAt(), now, RESEND_INTERVAL));
    }

    void assertInvitationResendAllowed(UUID userId, Instant now) {
        invitationTokens
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .ifPresent(last -> requireOutsideWindow(last.createdAt(), now, INVITATION_RESEND_INTERVAL));
    }

    private UUID consume(
            boolean consumed, boolean expired, java.util.function.Consumer<Instant> mark, UUID userId, Instant now) {
        if (consumed || expired) {
            throw new TokenExpiredException();
        }
        mark.accept(now);
        return userId;
    }

    private void requireOutsideWindow(Instant lastCreatedAt, Instant now, Duration interval) {
        var allowedAt = lastCreatedAt.plus(interval);
        if (now.isBefore(allowedAt)) {
            throw new RegistrationRateLimitedException(
                    Math.max(1, Duration.between(now, allowedAt).toSeconds()));
        }
    }

    private String generateRawToken() {
        var bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
