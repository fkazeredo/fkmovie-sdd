package com.fksoft.domain.payment;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes inbound payment webhooks (SPEC-0015): validates the HMAC signature on the raw body
 * before trusting it, parses the payload, and applies it idempotently — keyed on
 * {@code (paymentId, eventType)} — settling the payment and publishing the matching domain event for
 * booking to react to. The module never touches reservations (ADR 0006 boundary).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentWebhookHandler {

    private final PaymentSigner signer;
    private final WebhookJson json;
    private final PaymentRepository payments;
    private final PaymentWebhookEventRepository webhookEvents;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    /**
     * @throws InvalidWebhookSignatureException bad/missing signature (401);
     *     {@link InvalidWebhookPayloadException} malformed or unknown payload (422).
     */
    @Transactional
    public void handle(String rawBody, String signature) {
        if (!signer.matches(rawBody, signature)) {
            meterRegistry.counter("payment_webhook_signature_failures_total").increment();
            throw new InvalidWebhookSignatureException();
        }
        var payload = json.read(rawBody);
        if (webhookEvents.existsByPaymentIdAndEventType(payload.paymentId(), payload.eventType())) {
            meterRegistry
                    .counter("payment_webhooks_total", "outcome", "duplicate")
                    .increment();
            log.info("payment webhook duplicate paymentId={} eventType={}", payload.paymentId(), payload.eventType());
            return;
        }
        webhookEvents.save(new PaymentWebhookEvent(payload.paymentId(), payload.eventType(), clock.instant()));
        apply(payload);
        meterRegistry.counter("payment_webhooks_total", "outcome", "processed").increment();
        log.info("payment webhook processed paymentId={} eventType={}", payload.paymentId(), payload.eventType());
    }

    private void apply(WebhookPayload payload) {
        var payment = payments.findById(payload.paymentId()).orElseThrow(InvalidWebhookPayloadException::new);
        switch (payload.eventType()) {
            case "PAYMENT_SUCCEEDED" -> {
                payment.settle(PaymentOutcome.SUCCEEDED, clock.instant());
                events.publishEvent(new PaymentSucceeded(payment.id(), payment.reservationId(), payment.amountCents()));
            }
            case "PAYMENT_FAILED" -> {
                payment.settle(PaymentOutcome.FAILED, clock.instant());
                events.publishEvent(new PaymentFailed(payment.id(), payment.reservationId(), payment.amountCents()));
            }
            case "REFUND_SUCCEEDED" -> {
                payment.settle(PaymentOutcome.SUCCEEDED, clock.instant());
                events.publishEvent(new RefundSucceeded(payment.id(), payment.reservationId(), payment.amountCents()));
            }
            case "REFUND_FAILED" -> {
                payment.settle(PaymentOutcome.FAILED, clock.instant());
                events.publishEvent(new RefundFailed(payment.id(), payment.reservationId(), payment.amountCents()));
            }
            default -> throw new InvalidWebhookPayloadException();
        }
    }
}
