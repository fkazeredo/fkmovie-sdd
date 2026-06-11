package com.fksoft.application.auth;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Window-based lockout over the {@code login_attempts} table (SPEC-0003): 5 failures per
 * email or 20 per IP within 15 minutes lock the dimension until the oldest counted failure
 * leaves the window. DB-backed counting is sufficient for the single-instance deployment
 * (ADR 0002) — no in-memory or distributed limiter needed.
 */
@Component
class LoginRateLimiter {

    static final int EMAIL_THRESHOLD = 5;
    static final int IP_THRESHOLD = 20;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final LoginAttemptRepository attempts;
    private final MeterRegistry meterRegistry;

    LoginRateLimiter(LoginAttemptRepository attempts, MeterRegistry meterRegistry) {
        this.attempts = attempts;
        this.meterRegistry = meterRegistry;
    }

    /** Records a failed credential evaluation; it counts toward both window dimensions. */
    void recordFailure(String normalizedEmail, String ip, Instant now) {
        attempts.save(new LoginAttempt(normalizedEmail, ip, false, now));
    }

    /** Records a successful login for the audit trail; successes never count toward lockout. */
    void recordSuccess(String normalizedEmail, String ip, Instant now) {
        attempts.save(new LoginAttempt(normalizedEmail, ip, true, now));
    }

    /**
     * Rejects the login before credentials are evaluated; a 429 never records an attempt row,
     * otherwise lockouts would extend themselves forever.
     *
     * @throws LoginRateLimitedException with a precise {@code Retry-After}.
     */
    void checkAllowed(String normalizedEmail, String ip, Instant now) {
        var since = now.minus(WINDOW);
        requireBelowThreshold(
                attempts.recentFailureTimesByEmail(normalizedEmail, since, PageRequest.of(0, EMAIL_THRESHOLD)),
                EMAIL_THRESHOLD,
                now);
        requireBelowThreshold(
                attempts.recentFailureTimesByIp(ip, since, PageRequest.of(0, IP_THRESHOLD)), IP_THRESHOLD, now);
    }

    private void requireBelowThreshold(List<Instant> failureTimes, int threshold, Instant now) {
        if (failureTimes.size() < threshold) {
            return;
        }
        var oldestCounted = failureTimes.get(failureTimes.size() - 1);
        var retryAfterSeconds =
                Math.max(1, Duration.between(now, oldestCounted.plus(WINDOW)).toSeconds());
        meterRegistry.counter("auth.rate_limit.triggered").increment();
        throw new LoginRateLimitedException(retryAfterSeconds);
    }
}
