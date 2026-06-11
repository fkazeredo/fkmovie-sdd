package com.fksoft.application.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.application.notification.OutboxStatus;
import org.junit.jupiter.api.Test;

/** SPEC-0004 acceptance: register → verify → login, with the verification email enqueued. */
class RegistrationFlowIntegrationTest extends RegistrationIntegrationTestSupport {

    @Test
    void registerAutoLogsInAndEnqueuesVerificationEmail() throws Exception {
        var email = uniqueEmail();

        var response = register(email);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body())
                .contains("\"accessToken\":\"")
                .contains("\"role\":\"CUSTOMER\"")
                .contains("\"emailVerified\":false");
        assertThat(refreshCookieOf(response)).startsWith("refreshToken=");

        var enqueued = outboxEmails.findAll();
        assertThat(enqueued).hasSize(1);
        assertThat(enqueued.get(0).status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(enqueued.get(0).recipientEmail()).isEqualTo(email);
    }

    @Test
    void verifyEmailActivatesTheAccountAndIsSingleUse() throws Exception {
        var email = uniqueEmail();
        register(email);
        var token = tokenFromEmailTo(email);

        var verify = postJson("/api/users/verify-email", "{\"token\":\"" + token + "\"}");
        assertThat(verify.statusCode()).isEqualTo(200);
        assertThat(verify.body()).contains("\"emailVerified\":true").contains(email);

        var reuse = postJson("/api/users/verify-email", "{\"token\":\"" + token + "\"}");
        assertThat(reuse.statusCode()).isEqualTo(410);
        assertThat(reuse.body()).contains("\"code\":\"user.token-expired\"");
    }

    @Test
    void registeredCustomerCanLogIn() throws Exception {
        var email = uniqueEmail();
        register(email);

        var login = postJson("/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");

        assertThat(login.statusCode()).isEqualTo(200);
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        var email = uniqueEmail();
        register(email);

        var second = register(email);

        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(second.body()).contains("\"code\":\"user.email-taken\"");
    }

    @Test
    void malformedVerificationTokenIsRejected() throws Exception {
        var response = postJson("/api/users/verify-email", "{\"token\":\"not-a-real-token\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"user.token-invalid\"");
    }

    @Test
    void resendVerificationWithinAMinuteIsRateLimited() throws Exception {
        var email = uniqueEmail();
        register(email);

        var resend = postJson("/api/users/resend-verification", "{\"email\":\"" + email + "\"}");

        assertThat(resend.statusCode()).isEqualTo(429);
        assertThat(resend.body()).contains("\"code\":\"user.rate-limited\"");
        assertThat(resend.headers().firstValue("Retry-After")).isPresent();
    }
}
