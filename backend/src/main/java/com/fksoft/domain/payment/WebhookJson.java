package com.fksoft.domain.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

/**
 * JSON codec for payment webhook bodies (SPEC-0015). Owns its own {@link ObjectMapper} (configured
 * for ISO-8601 instants) so signing operates on a stable serialization, independent of the web
 * layer's converter. The mock signs exactly the string it sends; the handler verifies the bytes it
 * receives.
 */
@Component
public class WebhookJson {

    private final ObjectMapper mapper =
            new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public String write(WebhookPayload payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize webhook payload", ex);
        }
    }

    WebhookPayload read(String body) {
        try {
            return mapper.readValue(body, WebhookPayload.class);
        } catch (Exception ex) {
            throw new InvalidWebhookPayloadException();
        }
    }
}
