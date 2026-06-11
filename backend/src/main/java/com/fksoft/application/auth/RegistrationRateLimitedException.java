package com.fksoft.application.auth;

import com.fksoft.shared.error.BusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Resend-verification / forgot-password abuse (SPEC-0004: 1/min); carries Retry-After. */
public class RegistrationRateLimitedException extends BusinessException {

    private final long retryAfterSeconds;

    public RegistrationRateLimitedException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "user.rate-limited");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public Map<String, String> httpHeaders() {
        return Map.of("Retry-After", String.valueOf(retryAfterSeconds));
    }
}
