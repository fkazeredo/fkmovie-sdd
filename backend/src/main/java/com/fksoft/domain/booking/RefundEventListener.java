package com.fksoft.domain.booking;

import com.fksoft.domain.payment.RefundFailed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Watches refund settlement for cancellation follow-up (SPEC-0018). A failed refund is rare with the
 * mock but routine with real gateways: it raises an ERROR audit + metric for manual ops, without
 * touching the reservation (already CANCELLED). A successful refund needs no action — the {@code
 * payments} table is the ledger.
 */
@Component
@Slf4j
@RequiredArgsConstructor
class RefundEventListener {

    private final MeterRegistry meterRegistry;

    @TransactionalEventListener
    void on(RefundFailed event) {
        meterRegistry.counter("refunds_failed_total").increment();
        log.error(
                "refund failed reservationId={} paymentId={} amount={} — manual follow-up required",
                event.reservationId(),
                event.paymentId(),
                event.amountCents());
    }
}
