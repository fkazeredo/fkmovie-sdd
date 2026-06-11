package com.fksoft;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/**
 * Global error contract from SPEC-0001: unknown paths return the standard
 * {code, message, fields} payload and every response carries a correlation ID header.
 */
class GlobalErrorHandlingIntegrationTest extends AbstractIntegrationTest {

    @Test
    void unknownPathReturnsStandardNotFoundPayload() throws Exception {
        HttpResponse<String> response = get("/unknown");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"code\":\"not-found\"").contains("\"fields\":[]");
    }

    @Test
    void everyResponseCarriesACorrelationIdHeader() throws Exception {
        HttpResponse<String> response = get("/unknown");

        assertThat(response.headers().firstValue("X-Correlation-Id")).isPresent();
    }
}
