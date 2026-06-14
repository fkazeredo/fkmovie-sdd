package com.fksoft.application.cinema;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.application.auth.RegistrationIntegrationTestSupport;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** SPEC-0026/0007: admins list rooms to schedule a screening; the endpoint is admin-only. */
class RoomAdminIntegrationTest extends RegistrationIntegrationTestSupport {

    @Test
    void listsSeededRoomsForAdmin() throws Exception {
        var bearer = adminBearer();

        var response = get("/api/admin/rooms", "Authorization", bearer);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Room 1").contains("Room 2");
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        assertThat(get("/api/admin/rooms").statusCode()).isEqualTo(401);
    }

    private String adminBearer() throws IOException, InterruptedException {
        var email = uniqueEmail();
        seedAdmin(email);
        return bearerFor(email);
    }
}
