package com.fksoft.application.payment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional unit of work behind the mock delivery dispatcher (SPEC-0015, ADR 0006). Claiming and
 * per-job delivery each run in their own transaction (called cross-bean by {@link
 * MockPaymentDispatcher} so the proxy applies them). A due job POSTs a signed webhook to the
 * application's own endpoint and is then stamped delivered; a failed POST leaves it for the next poll.
 */
@Component
class MockPaymentDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentDeliveryWorker.class);

    private final MockPaymentJobRepository jobs;
    private final PaymentRepository payments;
    private final PaymentSigner signer;
    private final WebhookUrlResolver urlResolver;
    private final WebhookJson json;
    private final Clock clock;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    MockPaymentDeliveryWorker(
            MockPaymentJobRepository jobs,
            PaymentRepository payments,
            PaymentSigner signer,
            WebhookUrlResolver urlResolver,
            WebhookJson json,
            Clock clock) {
        this.jobs = jobs;
        this.payments = payments;
        this.signer = signer;
        this.urlResolver = urlResolver;
        this.json = json;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    List<UUID> claimDueIds() {
        return jobs.claimDue(clock.instant(), PageRequest.of(0, 50)).stream()
                .map(MockPaymentJob::id)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void deliver(UUID jobId) {
        var job = jobs.findById(jobId).orElse(null);
        if (job == null || job.isDelivered()) {
            return;
        }
        var payment = payments.findById(job.paymentId()).orElseThrow();
        var payload = new WebhookPayload(
                UUID.randomUUID(),
                eventType(payment.kind(), job.outcome()),
                payment.id(),
                payment.reservationId(),
                payment.amountCents(),
                clock.instant());
        try {
            var body = json.write(payload);
            var response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(urlResolver.mockWebhookUrl()))
                            .header("Content-Type", "application/json")
                            .header("X-Signature", signer.sign(body))
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 == 2) {
                job.markDelivered(clock.instant());
            } else {
                log.warn("mock webhook delivery non-2xx paymentId={} status={}", payment.id(), response.statusCode());
            }
        } catch (Exception ex) {
            log.warn("mock webhook delivery failed paymentId={}: {}", payment.id(), ex.getMessage());
        }
    }

    private static String eventType(PaymentKind kind, PaymentOutcome outcome) {
        var prefix = kind == PaymentKind.REFUND ? "REFUND" : "PAYMENT";
        return prefix + "_" + outcome.name();
    }
}
