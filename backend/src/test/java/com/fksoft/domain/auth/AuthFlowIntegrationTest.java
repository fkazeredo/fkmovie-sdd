package com.fksoft.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * SPEC-0003 acceptance: login issues access + refresh; refresh rotates single-use tokens;
 * reuse of a rotated token revokes the whole family; logout invalidates the refresh.
 */
class AuthFlowIntegrationTest extends AuthIntegrationTestSupport {

    @Test
    void loginRefreshAndReuseDetectionFlow() throws Exception {
        var user = seedActiveUser();

        var loginResponse = login(user.email(), PASSWORD);
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        assertThat(loginResponse.body())
                .contains("\"accessToken\":\"")
                .contains("\"accessTokenExpiresAt\":\"")
                .contains("\"email\":\"" + user.email() + "\"")
                .contains("\"role\":\"CUSTOMER\"");
        var setCookie = setCookieOf(loginResponse);
        assertThat(setCookie)
                .contains("refreshToken=")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Strict")
                .contains("Path=/api/auth");

        var me = get("/api/auth/me", "Authorization", "Bearer " + accessTokenOf(loginResponse));
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("\"email\":\"" + user.email() + "\"");

        var firstRefreshCookie = refreshCookieOf(loginResponse);
        var refreshResponse = postJson("/api/auth/refresh", "", "Cookie", firstRefreshCookie);
        assertThat(refreshResponse.statusCode()).isEqualTo(200);
        var rotatedCookie = refreshCookieOf(refreshResponse);
        assertThat(rotatedCookie).isNotEqualTo(firstRefreshCookie);

        var reuse = postJson("/api/auth/refresh", "", "Cookie", firstRefreshCookie);
        assertThat(reuse.statusCode()).isEqualTo(401);
        assertThat(reuse.body()).contains("\"code\":\"auth.token-reuse-detected\"");

        var cascaded = postJson("/api/auth/refresh", "", "Cookie", rotatedCookie);
        assertThat(cascaded.statusCode()).isEqualTo(401);
    }

    @Test
    void logoutRevokesThePresentedRefreshToken() throws Exception {
        var user = seedActiveUser();
        var loginResponse = login(user.email(), PASSWORD);
        var cookie = refreshCookieOf(loginResponse);

        var logout = postJson("/api/auth/logout", "", "Cookie", cookie);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(setCookieOf(logout)).contains("refreshToken=;").contains("Max-Age=0");

        var refreshAfterLogout = postJson("/api/auth/refresh", "", "Cookie", cookie);
        assertThat(refreshAfterLogout.statusCode()).isEqualTo(401);
    }

    @Test
    void authenticatedUnknownPathKeepsTheNotFoundContract() throws Exception {
        var user = seedActiveUser();
        var token = accessTokenOf(login(user.email(), PASSWORD));

        var response = get("/unknown", "Authorization", "Bearer " + token);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"code\":\"not-found\"").contains("\"fields\":[]");
    }
}
