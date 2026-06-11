package com.fksoft.application.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SPEC-0003 change-password: old password required; success revokes every refresh token. */
class ChangePasswordIntegrationTest extends AuthIntegrationTestSupport {

    private static final String NEW_PASSWORD = "brand-new-pass1";

    @Test
    void wrongCurrentPasswordIsRejected() throws Exception {
        var user = seedActiveUser();
        var token = accessTokenOf(login(user.email(), PASSWORD));

        var response = postJson(
                "/api/auth/change-password",
                "{\"currentPassword\":\"wrong\",\"newPassword\":\"" + NEW_PASSWORD + "\"}",
                "Authorization",
                "Bearer " + token);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"code\":\"auth.password-mismatch\"");
    }

    @Test
    void successSwapsTheCredentialAndRevokesExistingSessions() throws Exception {
        var user = seedActiveUser();
        var loginResponse = login(user.email(), PASSWORD);
        var token = accessTokenOf(loginResponse);
        var preChangeCookie = refreshCookieOf(loginResponse);

        var change = postJson(
                "/api/auth/change-password",
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}",
                "Authorization",
                "Bearer " + token);
        assertThat(change.statusCode()).isEqualTo(204);

        assertThat(login(user.email(), PASSWORD).statusCode()).isEqualTo(401);
        assertThat(login(user.email(), NEW_PASSWORD).statusCode()).isEqualTo(200);

        var refreshWithOldSession = postJson("/api/auth/refresh", "", "Cookie", preChangeCookie);
        assertThat(refreshWithOldSession.statusCode()).isEqualTo(401);
    }

    @Test
    void weakNewPasswordFailsTheGlobalValidationContract() throws Exception {
        var user = seedActiveUser();
        var token = accessTokenOf(login(user.email(), PASSWORD));

        var response = postJson(
                "/api/auth/change-password",
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"short\"}",
                "Authorization",
                "Bearer " + token);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"validation.error\"").contains("newPassword");
    }
}
