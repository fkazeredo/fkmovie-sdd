package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Lockout active (SPEC-0003); carries {@code Retry-After} so clients can back off correctly. */
public class LoginRateLimitedException extends BusinessException {

    private final long retryAfterSeconds;

    public LoginRateLimitedException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "auth.rate-limited");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public Map<String, String> httpHeaders() {
        return Map.of("Retry-After", String.valueOf(retryAfterSeconds));
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
