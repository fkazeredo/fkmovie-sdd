package com.fksoft.application.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Issues and consumes the single-use email-verification (24h) and password-reset (1h)
 * tokens of SPEC-0004. Tokens are 256-bit opaque values; only their SHA-256 hash is stored
 * (reusing {@link RefreshTokenService#sha256Hex}). Also enforces the 1-per-minute resend /
 * forgot-password rate limit.
 */
@Component
class VerificationTokenService {

    static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    static final Duration RESET_TTL = Duration.ofHours(1);
    static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);

    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final SecureRandom secureRandom = new SecureRandom();

    VerificationTokenService(
            EmailVerificationTokenRepository verificationTokens, PasswordResetTokenRepository resetTokens) {
        this.verificationTokens = verificationTokens;
        this.resetTokens = resetTokens;
    }

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
        if (token.isConsumed() || token.isExpired(now)) {
            throw new TokenExpiredException();
        }
        token.consume(now);
        return token.userId();
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
        if (token.isConsumed() || token.isExpired(now)) {
            throw new TokenExpiredException();
        }
        token.consume(now);
        return token.userId();
    }

    void assertVerificationResendAllowed(UUID userId, Instant now) {
        verificationTokens
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .ifPresent(last -> requireOutsideWindow(last.createdAt(), now));
    }

    void assertResetRequestAllowed(UUID userId, Instant now) {
        resetTokens
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .ifPresent(last -> requireOutsideWindow(last.createdAt(), now));
    }

    private void requireOutsideWindow(Instant lastCreatedAt, Instant now) {
        var allowedAt = lastCreatedAt.plus(RESEND_INTERVAL);
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
