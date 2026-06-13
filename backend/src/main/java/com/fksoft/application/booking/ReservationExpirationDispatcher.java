package com.fksoft.application.booking;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically releases stale holds (SPEC-0017, ADR 0002 single instance). Two sweeps per run: PENDING
 * past expiry → EXPIRED, AWAITING_PAYMENT past its deadline → CANCELLED; held seats return to FREE. The
 * scheduled poll claims rows (SKIP LOCKED) and drives the worker one reservation per transaction, so a
 * failing row never blocks the rest. A row failing three runs in a row raises an operational ERROR.
 * The method is public so tests can trigger a sweep deterministically (long interval keeps the poll
 * dormant), mirroring the mock payment dispatcher.
 */
@Component
public class ReservationExpirationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationDispatcher.class);
    private static final int CONSECUTIVE_FAILURE_ALERT = 3;

    private final ReservationExpirationWorker worker;
    private final MeterRegistry meterRegistry;
    private final Map<UUID, Integer> consecutiveFailures = new ConcurrentHashMap<>();

    ReservationExpirationDispatcher(ReservationExpirationWorker worker, MeterRegistry meterRegistry) {
        this.worker = worker;
        this.meterRegistry = meterRegistry;
    }

    /** Runs both expiration sweeps once (SPEC-0017); scheduled, and called explicitly in tests. */
    @Scheduled(fixedDelayString = "${app.booking.expiration-interval:PT30S}")
    public void sweep() {
        var sample = Timer.start(meterRegistry);
        var expiredIds = worker.claimExpiredIds();
        var timedOutIds = worker.claimPaymentTimedOutIds();
        meterRegistry.summary("expiration_job_batch_size").record(expiredIds.size() + timedOutIds.size());

        int expired = process(expiredIds, worker::expire);
        int cancelled = process(timedOutIds, worker::cancelForPaymentTimeout);
        int errors = expiredIds.size() + timedOutIds.size() - expired - cancelled;

        sample.stop(meterRegistry.timer("expiration_job_duration_seconds"));
        retainOnly(expiredIds, timedOutIds);
        log.info(
                "reservation expiration sweep scanned={} expired={} cancelled={} errors={}",
                expiredIds.size() + timedOutIds.size(),
                expired,
                cancelled,
                errors);
    }

    private int process(List<UUID> ids, Consumer<UUID> action) {
        int ok = 0;
        for (var id : ids) {
            try {
                action.accept(id);
                consecutiveFailures.remove(id);
                ok++;
            } catch (OptimisticLockingFailureException benign) {
                // A concurrent confirmation won the row; the guard will no-op it. Not an error.
                consecutiveFailures.remove(id);
                ok++;
                log.debug("reservation expiration skipped (concurrent update) reservationId={}", id);
            } catch (RuntimeException ex) {
                recordFailure(id, ex);
            }
        }
        return ok;
    }

    private void recordFailure(UUID reservationId, RuntimeException ex) {
        meterRegistry.counter("expiration_job_errors_total").increment();
        int runs = consecutiveFailures.merge(reservationId, 1, Integer::sum);
        if (runs >= CONSECUTIVE_FAILURE_ALERT) {
            log.error("reservation expiration failing repeatedly reservationId={} runs={}", reservationId, runs, ex);
        } else {
            log.warn("reservation expiration failed reservationId={}: {}", reservationId, ex.getMessage());
        }
    }

    private void retainOnly(List<UUID> expiredIds, List<UUID> timedOutIds) {
        Set<UUID> claimed = new HashSet<>(expiredIds);
        claimed.addAll(timedOutIds);
        consecutiveFailures.keySet().retainAll(claimed);
    }
}
