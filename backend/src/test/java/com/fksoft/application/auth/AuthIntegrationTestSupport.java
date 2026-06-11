package com.fksoft.application.auth;

import com.fksoft.AbstractIntegrationTest;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Plumbing for auth integration tests: user seeding with a cached bcrypt-12 hash (hashing is
 * ~150ms), JSON/cookie helpers and table cleanup so rate-limit windows never leak between
 * test classes (single native DELETE per table — the refresh_tokens self-FK forbids row-wise
 * deleteAll ordering).
 */
public abstract class AuthIntegrationTestSupport extends AbstractIntegrationTest {

    protected static final String PASSWORD = "secret123";

    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\":\"([^\"]+)\"");
    private static final Pattern REFRESH_COOKIE = Pattern.compile("refreshToken=([^;]*)");
    private static String cachedPasswordHash;

    @Autowired
    protected UserRepository users;

    @Autowired
    protected RefreshTokenRepository refreshTokens;

    @Autowired
    protected LoginAttemptRepository loginAttempts;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanAuthTables() {
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM login_attempts");
        jdbcTemplate.update("DELETE FROM users");
    }

    protected User seedActiveUser() {
        return seedUser(Role.CUSTOMER, false);
    }

    protected User seedUser(Role role, boolean disabled) {
        var user = new User(uniqueEmail(), passwordHash(), "Test User", role);
        if (disabled) {
            user.disable();
        }
        return users.save(user);
    }

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    protected String passwordHash() {
        if (cachedPasswordHash == null) {
            cachedPasswordHash = passwordEncoder.encode(PASSWORD);
        }
        return cachedPasswordHash;
    }

    protected HttpResponse<String> login(String email, String password, String... headerPairs)
            throws IOException, InterruptedException {
        var withContentType = new String[headerPairs.length];
        System.arraycopy(headerPairs, 0, withContentType, 0, headerPairs.length);
        return postJson("/api/auth/login", loginJson(email, password), withContentType);
    }

    protected static String loginJson(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    protected static String accessTokenOf(HttpResponse<String> response) {
        var matcher = ACCESS_TOKEN.matcher(response.body());
        if (!matcher.find()) {
            throw new AssertionError("No accessToken in body: " + response.body());
        }
        return matcher.group(1);
    }

    protected static String setCookieOf(HttpResponse<String> response) {
        return response.headers()
                .firstValue("set-cookie")
                .orElseThrow(() -> new AssertionError("No Set-Cookie header"));
    }

    protected static String refreshCookieOf(HttpResponse<String> response) {
        var matcher = REFRESH_COOKIE.matcher(setCookieOf(response));
        if (!matcher.find()) {
            throw new AssertionError("No refreshToken cookie in: " + setCookieOf(response));
        }
        return "refreshToken=" + matcher.group(1);
    }
}
