package com.fksoft.application.payment;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Async mock payment gateway (ADR 0006, SPEC-0015): records a PENDING payment and a delivery job
 * scheduled for {@code now + delay}; a worker later POSTs a signed webhook with the outcome. Default
 * outcome is SUCCEEDED; deterministic failure hooks (forceOutcome, or amount ending in …13) exist
 * for tests/staging and are disabled in prod.
 */
@Service
class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    private final PaymentRepository payments;
    private final MockPaymentJobRepository jobs;
    private final Clock clock;
    private final MeterRegistry meterRegistry;
    private final Duration delay;
    private final boolean failureRulesEnabled;

    MockPaymentGateway(
            PaymentRepository payments,
            MockPaymentJobRepository jobs,
            Clock clock,
            MeterRegistry meterRegistry,
            @Value("${app.payment.mock.delay:PT3S}") Duration delay,
            @Value("${app.payment.mock.failure-rules-enabled:true}") boolean failureRulesEnabled) {
        this.payments = payments;
        this.jobs = jobs;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
        this.delay = delay;
        this.failureRulesEnabled = failureRulesEnabled;
    }

    @Override
    @Transactional
    public PaymentRequestResult request(PaymentRequest request) {
        var now = clock.instant();
        var payment = payments.save(Payment.charge(request.reservationId(), request.amountCents(), now));
        var outcome = resolveOutcome(request.forceOutcome(), request.amountCents());
        jobs.save(new MockPaymentJob(payment.id(), now.plus(delay), outcome));
        meterRegistry
                .counter("payments_total", "kind", "CHARGE", "status", "PENDING")
                .increment();
        log.info(
                "payment requested paymentId={} reservationId={} amount={}",
                payment.id(),
                request.reservationId(),
                request.amountCents());
        return new PaymentRequestResult(payment.id(), PaymentStatus.PENDING);
    }

    @Override
    @Transactional
    public RefundRequestResult requestRefund(RefundRequest request) {
        var now = clock.instant();
        var payment = payments.save(Payment.refund(request.reservationId(), request.amountCents(), now));
        jobs.save(new MockPaymentJob(payment.id(), now.plus(delay), PaymentOutcome.SUCCEEDED));
        meterRegistry
                .counter("payments_total", "kind", "REFUND", "status", "PENDING")
                .increment();
        return new RefundRequestResult(payment.id(), PaymentStatus.PENDING);
    }

    private PaymentOutcome resolveOutcome(PaymentOutcome forced, int amountCents) {
        if (forced != null) {
            return forced;
        }
        if (failureRulesEnabled && amountCents % 100 == 13) {
            return PaymentOutcome.FAILED;
        }
        return PaymentOutcome.SUCCEEDED;
    }
}
