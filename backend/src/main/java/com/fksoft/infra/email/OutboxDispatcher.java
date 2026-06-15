package com.fksoft.infra.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the outbox and dispatches due emails (SPEC-0006). {@code fixedDelay} guarantees no
 * two polls overlap, so combined with {@code SELECT ... FOR UPDATE SKIP LOCKED} on the claim
 * query an email is never sent twice even though the work spans several transactions.
 *
 * <p>Disabled in tests ({@code app.notification.dispatch-enabled=false}) so they drive the
 * {@link OutboxSendWorker} deterministically instead of racing the scheduler.
 */
@Component
@ConditionalOnProperty(name = "app.notification.dispatch-enabled", matchIfMissing = true)
class OutboxDispatcher {

    private final OutboxSendWorker worker;

    OutboxDispatcher(OutboxSendWorker worker) {
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${app.notification.poll-interval:PT30S}")
    void dispatchPending() {
        for (var id : worker.claimDueIds()) {
            worker.process(id);
        }
    }
}
