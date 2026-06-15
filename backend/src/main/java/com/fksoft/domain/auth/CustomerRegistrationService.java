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
 * Customer self-service flows (SPEC-0004): register, resend verification, verify email,
 * forgot password and reset password. Reuses the auth module's {@link AccessTokens} port and
 * {@link RefreshTokenService} (same aggregate) for register auto-login and for revoking every
 * session on password reset.
 */
@Service
public class CustomerRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(CustomerRegistrationService.class);

    private final UserRepository users;
    private final VerificationTokenService tokens;
    private final AccessTokens accessTokens;
    private final RefreshTokenService refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;

    /** Collaborators injected by Spring — constructor injection only (CLAUDE.md). */
    public CustomerRegistrationService(
            UserRepository users,
            VerificationTokenService tokens,
            AccessTokens accessTokens,
            RefreshTokenService refreshTokens,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events,
            MeterRegistry meterRegistry) {
        this.users = users;
        this.tokens = tokens;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Registers a CUSTOMER (unverified) and auto-logs them in. A verification email is
     * triggered via {@link CustomerRegistered}. The account can browse but cannot reserve
     * until the email is verified (enforced from SPEC-0014).
     *
     * @throws EmailAlreadyRegisteredException when the email is already in use.
     */
    @Transactional
    public AuthService.AuthResult register(
            String name, String email, String rawPassword, String preferredLocale, String ip, String userAgent) {
        var normalizedEmail = User.normalizeEmail(email);
        if (users.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }
        var now = Instant.now();
        var user = users.save(
                User.newCustomer(normalizedEmail, passwordEncoder.encode(rawPassword), name.trim(), preferredLocale));

        var verificationToken = tokens.issueEmailVerification(user.id(), now);
        events.publishEvent(new CustomerRegistered(
                user.id(), user.email(), user.name(), user.preferredLocale(), verificationToken, now));

        var access = accessTokens.issue(user.id().toString(), user.role().name(), user.tenantId(), now);
        var refresh = refreshTokens.issueFor(user.id(), ip, userAgent, now);
        meterRegistry.counter("users.registered").increment();
        log.info("customer registered userId={}", user.id());
        return new AuthService.AuthResult(access.token(), access.expiresAt(), refresh.rawToken(), user);
    }

    /** Re-sends the verification email while unverified; silent + rate-limited (anti-enumeration). */
    @Transactional
    public void resendVerification(String email) {
        var user = users.findByEmail(User.normalizeEmail(email)).orElse(null);
        if (user == null || user.isEmailVerified()) {
            return;
        }
        var now = Instant.now();
        tokens.assertVerificationResendAllowed(user.id(), now);
        var verificationToken = tokens.issueEmailVerification(user.id(), now);
        events.publishEvent(new CustomerRegistered(
                user.id(), user.email(), user.name(), user.preferredLocale(), verificationToken, now));
    }

    /**
     * Verifies the email behind a single-use token.
     *
     * @throws TokenInvalidException unknown token; {@link TokenExpiredException} expired/consumed.
     */
    @Transactional
    public VerifyEmailResponse verifyEmail(String rawToken) {
        var now = Instant.now();
        var userId = consumeOrCountExpired(() -> tokens.consumeEmailVerification(rawToken, now));
        var user = users.findById(userId).orElseThrow(TokenInvalidException::new);
        user.verifyEmail(now);
        events.publishEvent(new EmailVerified(user.id(), now));
        meterRegistry.counter("users.email_verified").increment();
        return VerifyEmailResponse.from(user);
    }

    /** Always succeeds from the caller's view (SPEC-0004 anti-enumeration); emails only if known. */
    @Transactional
    public void forgotPassword(String email, String ip) {
        var user = users.findByEmail(User.normalizeEmail(email)).orElse(null);
        if (user == null) {
            meterRegistry
                    .counter("users.password_reset", "outcome", "unknown_email")
                    .increment();
            return;
        }
        var now = Instant.now();
        tokens.assertResetRequestAllowed(user.id(), now);
        var resetToken = tokens.issuePasswordReset(user.id(), ip, now);
        events.publishEvent(new PasswordResetRequested(
                user.id(), user.email(), user.name(), user.preferredLocale(), resetToken, now));
    }

    /** Resets the password and revokes every refresh token of the user (force re-login). */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        var now = Instant.now();
        var userId = consumeOrCountExpired(() -> tokens.consumePasswordReset(rawToken, now));
        var user = users.findById(userId).orElseThrow(TokenInvalidException::new);
        user.changePassword(passwordEncoder.encode(newPassword));
        refreshTokens.revokeAllForUser(userId, now);
        events.publishEvent(new PasswordResetCompleted(userId, now));
        meterRegistry.counter("users.password_reset", "outcome", "success").increment();
    }

    private UUID consumeOrCountExpired(java.util.function.Supplier<UUID> consume) {
        try {
            return consume.get();
        } catch (TokenExpiredException e) {
            meterRegistry.counter("users.token_expired").increment();
            throw e;
        }
    }
}
