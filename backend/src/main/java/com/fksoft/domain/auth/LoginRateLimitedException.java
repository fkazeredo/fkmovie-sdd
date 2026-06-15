package com.fksoft.domain.auth;

import com.fksoft.domain.error.DomainException;
import com.fksoft.domain.error.RateLimited;
import java.time.Duration;

/** Lockout active (SPEC-0003); states how long to wait so clients can back off correctly. */
public class LoginRateLimitedException extends DomainException implements RateLimited {

    private final long retryAfterSeconds;

    public LoginRateLimitedException(long retryAfterSeconds) {
        super("auth.rate-limited");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public Duration retryAfter() {
        return Duration.ofSeconds(retryAfterSeconds);
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
