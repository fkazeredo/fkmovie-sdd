package com.fksoft.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Security chain contract: protected by default, actuator/api-docs public, errors localized. */
class SecurityBaselineIntegrationTest extends AuthIntegrationTestSupport {

    @Test
    void protectedEndpointWithoutTokenReturns401InTheStandardShape() throws Exception {
        var response = get("/api/auth/me");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body())
                .contains("\"code\":\"auth.unauthenticated\"")
                .contains("\"fields\":[]");
        assertThat(response.headers().firstValue("X-Correlation-Id")).isPresent();
    }

    @Test
    void actuatorAndApiDocsStayPublic() throws Exception {
        assertThat(get("/actuator/health/liveness").statusCode()).isEqualTo(200);
        assertThat(get("/actuator/prometheus").statusCode()).isEqualTo(200);
        var apiDocs = get("/v3/api-docs");
        assertThat(apiDocs.statusCode()).isEqualTo(200);
        assertThat(apiDocs.body()).contains("/api/auth/login").contains("/api/users/register");
    }

    @Test
    void errorsAreLocalizedByAcceptLanguage() throws Exception {
        var response = login(uniqueEmail(), "wrong-pass1", "Accept-Language", "pt-BR");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("E-mail ou senha inválidos.");
    }
}
