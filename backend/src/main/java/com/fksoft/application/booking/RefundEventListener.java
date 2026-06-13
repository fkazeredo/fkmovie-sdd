package com.fksoft.application.booking;

import com.fksoft.application.payment.RefundFailed;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Watches refund settlement for cancellation follow-up (SPEC-0018). A failed refund is rare with the
 * mock but routine with real gateways: it raises an ERROR audit + metric for manual ops, without
 * touching the reservation (already CANCELLED). A successful refund needs no action — the {@code
 * payments} table is the ledger.
 */
@Component
class RefundEventListener {

    private static final Logger log = LoggerFactory.getLogger(RefundEventListener.class);

    private final MeterRegistry meterRegistry;

    RefundEventListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

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
