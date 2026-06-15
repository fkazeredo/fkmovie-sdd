package com.fksoft.infra.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Resolves the application's own mock-webhook URL for the delivery worker (SPEC-0015). The mock
 * delivers over HTTP to be production-shaped; the self-call targets the live embedded server port
 * ({@code local.server.port}, set in both prod and tests) or the configured base URL as a fallback.
 */
@Component
class WebhookUrlResolver {

    private static final String PATH = "/api/webhooks/payments/mock";

    private final Environment environment;
    private final String baseUrl;

    WebhookUrlResolver(
            Environment environment, @Value("${app.payment.webhook-base-url:http://localhost:8080}") String baseUrl) {
        this.environment = environment;
        this.baseUrl = baseUrl;
    }

    String mockWebhookUrl() {
        var port = environment.getProperty("local.server.port");
        var base = port != null ? "http://localhost:" + port : baseUrl;
        return base + PATH;
    }
}
