package com.fksoft;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/**
 * Liveness/readiness contract from SPEC-0001: both probes answer 200 {"status":"UP"} while
 * the application and its database are up.
 */
class HealthEndpointsIntegrationTest extends AbstractIntegrationTest {

    @Test
    void livenessReturnsUpWhileApplicationIsRunning() throws Exception {
        HttpResponse<String> response = get("/actuator/health/liveness");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void readinessReturnsUpWhileDatabaseIsReachable() throws Exception {
        HttpResponse<String> response = get("/actuator/health/readiness");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }
}
