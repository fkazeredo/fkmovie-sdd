package com.fksoft.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SPEC-0003: disabled users cannot log in (403; the account is preserved, never deleted). */
class DisabledUserIntegrationTest extends AuthIntegrationTestSupport {

    @Test
    void disabledUserWithCorrectPasswordGets403() throws Exception {
        var user = seedUser(Role.CUSTOMER, true);

        var response = login(user.email(), PASSWORD);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"code\":\"auth.disabled\"");
    }
}
