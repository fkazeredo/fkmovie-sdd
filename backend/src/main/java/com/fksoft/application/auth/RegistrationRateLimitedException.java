package com.fksoft.application.auth;

import com.fksoft.shared.error.DomainException;
import com.fksoft.shared.error.RateLimited;
import java.time.Duration;

/** Resend-verification / forgot-password abuse (SPEC-0004: 1/min); states the wait before retry. */
public class RegistrationRateLimitedException extends DomainException implements RateLimited {

    private final long retryAfterSeconds;

    public RegistrationRateLimitedException(long retryAfterSeconds) {
        super("user.rate-limited");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public Duration retryAfter() {
        return Duration.ofSeconds(retryAfterSeconds);
    }
}
