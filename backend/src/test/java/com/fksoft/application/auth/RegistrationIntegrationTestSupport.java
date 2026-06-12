package com.fksoft.application.auth;

import com.fksoft.AbstractIntegrationTest;
import com.fksoft.application.notification.OutboxEmail;
import com.fksoft.application.notification.OutboxEmailRepository;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Plumbing for SPEC-0004/0005/0006 integration tests: HTTP helpers, table cleanup, admin
 * seeding, and extraction of the raw token from the email the outbox enqueued (the only place
 * the raw token surfaces after registration/invitation).
 */
public abstract class RegistrationIntegrationTestSupport extends AbstractIntegrationTest {

    protected static final String PASSWORD = "secret123";

    private static final Pattern TOKEN_PARAM = Pattern.compile("token=([A-Za-z0-9_-]+)");
    private static final Pattern REFRESH_COOKIE = Pattern.compile("refreshToken=([^;]*)");

    @Autowired
    protected OutboxEmailRepository outboxEmails;

    @Autowired
    protected UserRepository users;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM outbox_emails");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM email_verification_tokens");
        jdbcTemplate.update("DELETE FROM password_reset_tokens");
        jdbcTemplate.update("DELETE FROM invitation_tokens");
        jdbcTemplate.update("DELETE FROM login_attempts");
        jdbcTemplate.update("DELETE FROM users");
    }

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    protected HttpResponse<String> register(String email, String... headerPairs)
            throws IOException, InterruptedException {
        var body = "{\"name\":\"Test User\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD);
        return postJson("/api/users/register", body, headerPairs);
    }

    /** Seeds an ACTIVE admin (verified) able to call the admin API. */
    protected User seedAdmin(String email) {
        var admin = new User(email, passwordEncoder.encode(PASSWORD), "Admin", Role.ADMIN);
        admin.verifyEmail(Instant.now());
        return users.save(admin);
    }

    /** Logs in and returns the {@code Authorization: Bearer ...} value for the given user. */
    protected String bearerFor(String email) throws IOException, InterruptedException {
        var login = postJson("/api/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        return "Bearer " + accessTokenOf(login);
    }

    protected String tokenFromEmailTo(String recipient) {
        var link = outboxEmails.findAll().stream()
                .filter(email -> email.recipientEmail().equalsIgnoreCase(recipient))
                .reduce((first, second) -> second)
                .map(OutboxEmail::payload)
                .map(payload -> payload.get("link"))
                .orElseThrow(() -> new AssertionError("No email enqueued for " + recipient));
        var matcher = TOKEN_PARAM.matcher(link);
        if (!matcher.find()) {
            throw new AssertionError("No token in link: " + link);
        }
        return matcher.group(1);
    }

    protected static String accessTokenOf(HttpResponse<String> response) {
        var matcher = Pattern.compile("\"accessToken\":\"([^\"]+)\"").matcher(response.body());
        if (!matcher.find()) {
            throw new AssertionError("No accessToken in body: " + response.body());
        }
        return matcher.group(1);
    }

    protected static String refreshCookieOf(HttpResponse<String> response) {
        var setCookie = response.headers()
                .firstValue("set-cookie")
                .orElseThrow(() -> new AssertionError("No Set-Cookie header"));
        var matcher = REFRESH_COOKIE.matcher(setCookie);
        if (!matcher.find()) {
            throw new AssertionError("No refreshToken cookie in: " + setCookie);
        }
        return "refreshToken=" + matcher.group(1);
    }
}
