package com.fksoft.application.api;

import com.fksoft.domain.payment.PaymentWebhookHandler;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock payment webhook endpoint (SPEC-0015, ADR 0006): public path, HMAC-protected, no JWT
 * (gateways hold no user token). A real provider adds its own path (e.g. {@code .../stripe}). Always
 * 200 on accepted/duplicate; 401 on bad signature, 422 on malformed body (via the handler).
 */
@RestController
class MockPaymentWebhookController {

    private final PaymentWebhookHandler handler;

    MockPaymentWebhookController(PaymentWebhookHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/api/webhooks/payments/mock")
    @ResponseStatus(HttpStatus.OK)
    void receive(@RequestBody String body, @RequestHeader(value = "X-Signature", required = false) String signature) {
        handler.handle(body, signature);
    }
}
