package com.fksoft.application.payment;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the mock delivery queue and delivers due webhooks (SPEC-0015). {@code fixedDelay} guarantees
 * no two polls overlap; the worker's claim/deliver are called cross-bean so their transactions apply
 * (mirrors the outbox dispatcher).
 */
@Component
class MockPaymentDispatcher {

    private final MockPaymentDeliveryWorker worker;

    MockPaymentDispatcher(MockPaymentDeliveryWorker worker) {
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${app.payment.mock.poll-interval:PT1S}")
    void deliverDue() {
        for (var jobId : worker.claimDueIds()) {
            worker.deliver(jobId);
        }
    }
}
