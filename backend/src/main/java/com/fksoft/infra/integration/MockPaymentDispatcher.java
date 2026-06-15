package com.fksoft.infra.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the mock delivery queue and delivers due webhooks (SPEC-0015). {@code fixedDelay} guarantees
 * no two polls overlap; the worker's claim/deliver are called cross-bean so their transactions apply
 * (mirrors the outbox dispatcher). The method is public so tests can drive delivery deterministically
 * (tests set a long poll interval and trigger it explicitly, avoiding scheduler races).
 */
@Component
@RequiredArgsConstructor
public class MockPaymentDispatcher {

    private final MockPaymentDeliveryWorker worker;

    @Scheduled(fixedDelayString = "${app.payment.mock.poll-interval:PT1S}")
    public void deliverDue() {
        for (var jobId : worker.claimDueIds()) {
            worker.deliver(jobId);
        }
    }
}
