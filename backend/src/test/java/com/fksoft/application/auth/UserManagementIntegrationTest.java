package com.fksoft.application.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SPEC-0005 acceptance: invite → accept → login; admin-only access; disable guards. */
class UserManagementIntegrationTest extends RegistrationIntegrationTestSupport {

    private static final String OPERATOR_PASSWORD = "operatorpass1";

    @Test
    void adminInvitesOperatorWhoAcceptsAndLogsIn() throws Exception {
        var adminEmail = uniqueEmail();
        seedAdmin(adminEmail);
        var adminBearer = bearerFor(adminEmail);
        var operatorEmail = uniqueEmail();

        var invite = postJson(
                "/api/admin/users",
                "{\"email\":\"%s\",\"name\":\"Maria\",\"role\":\"OPERATOR\"}".formatted(operatorEmail),
                "Authorization",
                adminBearer);
        assertThat(invite.statusCode()).isEqualTo(201);
        assertThat(invite.body())
                .contains("\"role\":\"OPERATOR\"")
                .contains("\"status\":\"DISABLED\"")
                .contains("\"emailVerified\":false");

        // Operator cannot log in before accepting.
        assertThat(postJson(
                                "/api/auth/login",
                                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(operatorEmail, OPERATOR_PASSWORD))
                        .statusCode())
                .isEqualTo(401);

        var token = tokenFromEmailTo(operatorEmail);
        var accept = postJson(
                "/api/users/accept-invitation",
                "{\"token\":\"%s\",\"password\":\"%s\",\"name\":\"Maria Silva\"}".formatted(token, OPERATOR_PASSWORD));
        assertThat(accept.statusCode()).isEqualTo(200);
        assertThat(accept.body()).contains("\"accessToken\":\"").contains("\"role\":\"OPERATOR\"");

        // Now the operator can log in with the chosen password.
        assertThat(postJson(
                                "/api/auth/login",
                                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(operatorEmail, OPERATOR_PASSWORD))
                        .statusCode())
                .isEqualTo(200);
    }

    @Test
    void invitingAnExistingEmailIsRejected() throws Exception {
        var adminEmail = uniqueEmail();
        seedAdmin(adminEmail);
        var adminBearer = bearerFor(adminEmail);

        var dup = postJson(
                "/api/admin/users",
                "{\"email\":\"%s\",\"name\":\"Duplicate\",\"role\":\"OPERATOR\"}".formatted(adminEmail),
                "Authorization",
                adminBearer);

        assertThat(dup.statusCode()).isEqualTo(409);
        assertThat(dup.body()).contains("\"code\":\"user.email-taken\"");
    }

    @Test
    void acceptInvitationWithExpiredOrUnknownTokenFails() throws Exception {
        var response = postJson(
                "/api/users/accept-invitation",
                "{\"token\":\"not-a-real-token\",\"password\":\"%s\",\"name\":\"Valid Name\"}"
                        .formatted(OPERATOR_PASSWORD));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"user.token-invalid\"");
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        var customerEmail = uniqueEmail();
        register(customerEmail);
        var customerBearer = "Bearer "
                + accessTokenOf(postJson(
                        "/api/auth/login",
                        "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(customerEmail, PASSWORD)));

        var list = get("/api/admin/users", "Authorization", customerBearer);
        assertThat(list.statusCode()).isEqualTo(403);
        assertThat(list.body()).contains("\"code\":\"auth.forbidden\"");

        var noToken = get("/api/admin/users");
        assertThat(noToken.statusCode()).isEqualTo(401);
    }

    @Test
    void listIsPaginatedAndFilterable() throws Exception {
        var adminEmail = uniqueEmail();
        seedAdmin(adminEmail);
        var adminBearer = bearerFor(adminEmail);
        for (int i = 0; i < 3; i++) {
            postJson(
                    "/api/admin/users",
                    "{\"email\":\"%s\",\"name\":\"Op\",\"role\":\"OPERATOR\"}".formatted(uniqueEmail()),
                    "Authorization",
                    adminBearer);
        }

        var page = get("/api/admin/users?role=OPERATOR&page=0&size=2", "Authorization", adminBearer);

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body())
                .contains("\"totalElements\":3")
                .contains("\"size\":2")
                .contains("\"totalPages\":2");
    }

    @Test
    void adminCannotDisableOwnAccountButCanDisableAnotherAdmin() throws Exception {
        var adminEmail = uniqueEmail();
        var admin = seedAdmin(adminEmail);
        var adminBearer = bearerFor(adminEmail);

        var disableSelf = postJson("/api/admin/users/" + admin.id() + "/disable", "", "Authorization", adminBearer);
        assertThat(disableSelf.statusCode()).isEqualTo(409);
        assertThat(disableSelf.body()).contains("\"code\":\"user.cannot-disable-self\"");

        // With two admins present, disabling the other one is allowed (the last-admin guard
        // itself is covered directly in UserManagementPolicyTest).
        var other = seedAdmin(uniqueEmail());
        var disableOther = postJson("/api/admin/users/" + other.id() + "/disable", "", "Authorization", adminBearer);
        assertThat(disableOther.statusCode()).isEqualTo(200);
        assertThat(disableOther.body()).contains("\"status\":\"DISABLED\"");
    }
}
