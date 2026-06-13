package com.fksoft;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/**
 * Global error contract: since SPEC-0003 the API is protected by default, so an
 * unauthenticated request to an unknown path gets the 401 contract (the authenticated 404
 * contract is covered by AuthFlowIntegrationTest). Every response carries a correlation ID.
 */
class GlobalErrorHandlingIntegrationTest extends AbstractIntegrationTest {

    @Test
    void unknownPathWithoutTokenReturnsStandardUnauthenticatedPayload() throws Exception {
        HttpResponse<String> response = get("/unknown");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body())
                .contains("\"code\":\"auth.unauthenticated\"")
                .contains("\"fields\":[]");
    }

    @Test
    void everyResponseCarriesACorrelationIdHeader() throws Exception {
        HttpResponse<String> response = get("/unknown");

        assertThat(response.headers().firstValue("X-Correlation-Id")).isPresent();
    }

    @Test
    void malformedQueryParamReturns400NotFramework500() throws Exception {
        // public endpoint (SPEC-0010); a non-UUID movieId is a type mismatch, must be 400 not 500.
        HttpResponse<String> response = get("/api/screenings?movieId=not-a-uuid");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"validation.error\"").contains("movieId");
    }
}
