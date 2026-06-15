package com.fksoft.application.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.AbstractIntegrationTest;
import com.fksoft.infra.integration.MockPaymentDispatcher;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.event.TransactionalEventListener;

/** SPEC-0015: the mock settles via a signed webhook; the endpoint verifies HMAC and is idempotent. */
@Import(PaymentWebhookIntegrationTest.CaptorConfig.class)
class PaymentWebhookIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentGateway gateway;

    @Autowired
    private MockPaymentDispatcher dispatcher;

    @Autowired
    private PaymentRepository payments;

    @Autowired
    private PaymentWebhookEventRepository webhookEvents;

    @Autowired
    private MockPaymentJobRepository jobs;

    @Autowired
    private PaymentSigner signer;

    @Autowired
    private WebhookJson json;

    @Autowired
    private PaymentEventCaptor captor;

    @AfterEach
    void clean() {
        jobs.deleteAll();
        webhookEvents.deleteAll();
        payments.deleteAll();
        captor.succeeded.clear();
        captor.failed.clear();
    }

    @Test
    void chargeSettlesViaWebhookExactlyOnce() {
        var paymentId =
                gateway.request(PaymentRequest.of(UUID.randomUUID(), 3000)).paymentId();

        dispatcher.deliverDue();

        assertThat(captor.succeeded).filteredOn(id -> id.equals(paymentId)).hasSize(1);
        assertThat(payments.findById(paymentId).orElseThrow().status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void forcedFailureSettlesAsFailed() {
        var paymentId = gateway.request(new PaymentRequest(UUID.randomUUID(), 3000, PaymentOutcome.FAILED))
                .paymentId();

        dispatcher.deliverDue();

        assertThat(payments.findById(paymentId).orElseThrow().status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void duplicateWebhookIsANoOp() throws Exception {
        var payment = payments.save(Payment.charge(UUID.randomUUID(), 3000, Instant.now()));
        var body = signedBody(payment.id());

        assertThat(postWebhook(body).statusCode()).isEqualTo(200);
        assertThat(postWebhook(body).statusCode()).isEqualTo(200);

        assertThat(captor.succeeded).filteredOn(id -> id.equals(payment.id())).hasSize(1);
    }

    @Test
    void invalidSignatureIsRejectedAndNotProcessed() throws Exception {
        var payment = payments.save(Payment.charge(UUID.randomUUID(), 3000, Instant.now()));
        var body = bodyFor(payment.id());

        var response = postJson("/api/webhooks/payments/mock", body, "X-Signature", "not-a-valid-signature");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"code\":\"payment.invalid-signature\"");
        assertThat(payments.findById(payment.id()).orElseThrow().status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void malformedPayloadIsRejected() throws Exception {
        var body = "{not json";

        var response = postJson("/api/webhooks/payments/mock", body, "X-Signature", signer.sign(body));

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("\"code\":\"payment.invalid-payload\"");
    }

    private java.net.http.HttpResponse<String> postWebhook(String body) throws Exception {
        return postJson("/api/webhooks/payments/mock", body, "X-Signature", signer.sign(body));
    }

    private String signedBody(UUID paymentId) throws Exception {
        return bodyFor(paymentId);
    }

    private String bodyFor(UUID paymentId) {
        return json.write(new WebhookPayload(
                UUID.randomUUID(), "PAYMENT_SUCCEEDED", paymentId, UUID.randomUUID(), 3000, Instant.now()));
    }

    @TestConfiguration
    static class CaptorConfig {
        @Bean
        PaymentEventCaptor paymentEventCaptor() {
            return new PaymentEventCaptor();
        }
    }

    static class PaymentEventCaptor {
        private final List<UUID> succeeded = new CopyOnWriteArrayList<>();
        private final List<UUID> failed = new CopyOnWriteArrayList<>();

        @TransactionalEventListener
        void onSucceeded(PaymentSucceeded event) {
            succeeded.add(event.paymentId());
        }

        @TransactionalEventListener
        void onFailed(PaymentFailed event) {
            failed.add(event.paymentId());
        }
    }
}
