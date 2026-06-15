package com.fksoft.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SPEC-0004 forgot/reset flow: anti-enumeration, single-use token, session revocation. */
class PasswordResetIntegrationTest extends RegistrationIntegrationTestSupport {

    private static final String NEW_PASSWORD = "brand-new-pass1";

    @Test
    void forgotPasswordWithUnknownEmailStillReturns200() throws Exception {
        var response = postJson("/api/users/forgot-password", "{\"email\":\"nobody@test.local\"}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(outboxEmails.findAll()).isEmpty();
    }

    @Test
    void resetPasswordChangesCredentialAndRevokesAllRefreshTokens() throws Exception {
        var email = uniqueEmail();
        var registration = register(email);
        var oldRefreshCookie = refreshCookieOf(registration);

        var forgot = postJson("/api/users/forgot-password", "{\"email\":\"" + email + "\"}");
        assertThat(forgot.statusCode()).isEqualTo(200);
        var resetToken = tokenFromEmailTo(email);

        var reset = postJson(
                "/api/users/reset-password",
                "{\"token\":\"" + resetToken + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}");
        assertThat(reset.statusCode()).isEqualTo(200);

        // Old password rejected, new password accepted.
        assertThat(postJson("/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")
                        .statusCode())
                .isEqualTo(401);
        assertThat(postJson("/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + NEW_PASSWORD + "\"}")
                        .statusCode())
                .isEqualTo(200);

        // The refresh token issued at registration was revoked.
        var refresh = postJson("/api/auth/refresh", "", "Cookie", oldRefreshCookie);
        assertThat(refresh.statusCode()).isEqualTo(401);
    }

    @Test
    void resetWithWeakPasswordFailsTheGlobalValidationContract() throws Exception {
        var email = uniqueEmail();
        register(email);
        postJson("/api/users/forgot-password", "{\"email\":\"" + email + "\"}");
        var resetToken = tokenFromEmailTo(email);

        var reset =
                postJson("/api/users/reset-password", "{\"token\":\"" + resetToken + "\",\"newPassword\":\"short\"}");

        assertThat(reset.statusCode()).isEqualTo(400);
        assertThat(reset.body()).contains("\"code\":\"validation.error\"").contains("newPassword");
    }
}
