package com.fksoft.domain.auth;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication use cases (SPEC-0003): login, refresh, logout, change-password and current
 * user. Coordinates rate limiting, credential checks, token issuance and event publication;
 * the domain rules live in the entities and collaborators.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final RefreshTokenService refreshTokens;
    private final AccessTokens accessTokens;
    private final LoginRateLimiter rateLimiter;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;

    /** Collaborators injected by Spring — constructor injection only (CLAUDE.md). */
    public AuthService(
            UserRepository users,
            RefreshTokenService refreshTokens,
            AccessTokens accessTokens,
            LoginRateLimiter rateLimiter,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events,
            MeterRegistry meterRegistry) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.rateLimiter = rateLimiter;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Authenticates by email/password and issues a new token pair. Wrong password and unknown
     * email are indistinguishable; the disabled state is only revealed after the password
     * matched (no user enumeration). Every credential evaluation is recorded for the rate
     * limiter; a 429 records nothing. The failure exceptions must NOT roll back the
     * transaction, otherwise the login_attempts row would be erased and the rate limiter
     * blinded (SPEC-0003).
     */
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, UserDisabledException.class})
    public AuthResult login(String email, String rawPassword, String ip, String userAgent) {
        var normalizedEmail = User.normalizeEmail(email);
        var now = Instant.now();
        try {
            rateLimiter.checkAllowed(normalizedEmail, ip, now);
        } catch (LoginRateLimitedException e) {
            countLogin("rate_limited");
            logAttempt(normalizedEmail, ip, userAgent, "rate_limited");
            throw e;
        }

        var user = users.findByEmail(normalizedEmail).orElse(null);
        var passwordMatches = user != null && passwordEncoder.matches(rawPassword, user.passwordHash());
        if (user == null || !passwordMatches) {
            rateLimiter.recordFailure(normalizedEmail, ip, now);
            countLogin("invalid_credentials");
            logAttempt(normalizedEmail, ip, userAgent, "invalid_credentials");
            throw new InvalidCredentialsException();
        }
        if (user.status() == UserStatus.DISABLED) {
            rateLimiter.recordFailure(normalizedEmail, ip, now);
            countLogin("disabled");
            logAttempt(normalizedEmail, ip, userAgent, "disabled");
            throw new UserDisabledException();
        }

        rateLimiter.recordSuccess(normalizedEmail, ip, now);
        var access = accessTokens.issue(user.id().toString(), user.role().name(), user.tenantId(), now);
        var refresh = refreshTokens.issueFor(user.id(), ip, userAgent, now);
        events.publishEvent(new UserLoggedIn(user.id(), ip, userAgent, now));
        countLogin("success");
        logAttempt(normalizedEmail, ip, userAgent, "success");
        return new AuthResult(access.token(), access.expiresAt(), refresh.rawToken(), user);
    }

    /**
     * Exchanges a refresh token for a new pair (single-use rotation). Reuse of a revoked
     * token revokes the whole session family; a disabled user cannot refresh. The reuse
     * exception must NOT roll back the transaction, otherwise the cascade revocation —
     * the whole point of the theft response — would be erased (SPEC-0003).
     */
    @Transactional(noRollbackFor = TokenReuseDetectedException.class)
    public AuthResult refresh(String rawRefreshToken, String ip, String userAgent) {
        var now = Instant.now();
        RefreshTokenService.RotationResult rotation;
        try {
            rotation = refreshTokens.rotate(rawRefreshToken, ip, userAgent, now);
        } catch (InvalidRefreshTokenException | TokenReuseDetectedException e) {
            meterRegistry.counter("auth.refresh", "outcome", e.code()).increment();
            throw e;
        }
        var user = users.findById(rotation.userId()).orElseThrow(InvalidRefreshTokenException::new);
        user.requireActive();
        var access = accessTokens.issue(user.id().toString(), user.role().name(), user.tenantId(), now);
        meterRegistry.counter("auth.refresh", "outcome", "success").increment();
        return new AuthResult(
                access.token(), access.expiresAt(), rotation.issued().rawToken(), user);
    }

    /** Revokes the presented refresh token; unknown tokens are ignored (idempotent logout). */
    @Transactional
    public void logout(String rawRefreshToken) {
        var now = Instant.now();
        refreshTokens.revoke(rawRefreshToken, now).ifPresent(userId -> {
            events.publishEvent(new UserLoggedOut(userId, now));
            meterRegistry.counter("auth.logout").increment();
        });
    }

    /**
     * Changes the password after verifying the current one, and revokes every refresh token
     * of the user — other sessions must re-authenticate (decision recorded in SPEC-0003).
     *
     * @throws PasswordMismatchException when the current password is wrong.
     */
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        var user = users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));
        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new PasswordMismatchException();
        }
        var now = Instant.now();
        user.changePassword(passwordEncoder.encode(newPassword));
        refreshTokens.revokeAllForUser(userId, now);
        events.publishEvent(new PasswordChanged(userId, now));
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse currentUser(UUID userId) {
        var user = users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));
        return UserSummaryResponse.from(user);
    }

    private void countLogin(String outcome) {
        meterRegistry.counter("auth.login", "outcome", outcome).increment();
    }

    private void logAttempt(String normalizedEmail, String ip, String userAgent, String outcome) {
        log.info(
                "login attempt email={} ip={} userAgent={} outcome={}",
                HashedEmail.of(normalizedEmail),
                ip,
                userAgent,
                outcome);
    }

    /** Outcome of login/refresh: the access token, its expiry, the raw refresh and the user. */
    public record AuthResult(String accessToken, Instant accessTokenExpiresAt, String refreshToken, User user) {

        /** The authenticated user as a transport-free summary, for the delivery layer. */
        public UserSummaryResponse userSummary() {
            return UserSummaryResponse.from(user);
        }
    }
}
