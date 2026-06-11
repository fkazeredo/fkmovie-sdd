package com.fksoft.application.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** SPEC-0003: 5 failures/email/15min → 429 with Retry-After; 20 failures/IP/15min; time reset. */
class LoginRateLimitIntegrationTest extends AuthIntegrationTestSupport {

    @Test
    void sixthFailureForTheSameEmailIsRateLimitedWithoutRecordingAnAttempt() throws Exception {
        var user = seedActiveUser();
        for (int i = 0; i < 5; i++) {
            assertThat(login(user.email(), "wrong-password").statusCode()).isEqualTo(401);
        }

        var limited = login(user.email(), PASSWORD);

        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.body()).contains("\"code\":\"auth.rate-limited\"");
        var retryAfter = limited.headers().firstValue("Retry-After").orElseThrow();
        assertThat(Long.parseLong(retryAfter)).isBetween(1L, 900L);
        assertThat(loginAttempts.count()).isEqualTo(5);
    }

    @Test
    void lockoutExpiresWhenFailuresLeaveTheWindow() throws Exception {
        var user = seedActiveUser();
        var backdated = Instant.now().minus(Duration.ofMinutes(16));
        for (int i = 0; i < 5; i++) {
            loginAttempts.save(new LoginAttempt(user.email(), "127.0.0.1", false, backdated));
        }

        var response = login(user.email(), PASSWORD);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void twentyFailuresFromOneIpLockTheIpAcrossEmails() throws Exception {
        var user = seedActiveUser();
        var now = Instant.now();
        for (int i = 0; i < 20; i++) {
            loginAttempts.save(new LoginAttempt("other-" + i + "@test.local", "127.0.0.1", false, now));
        }

        var limited = login(user.email(), PASSWORD);

        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.body()).contains("\"code\":\"auth.rate-limited\"");
    }
}
