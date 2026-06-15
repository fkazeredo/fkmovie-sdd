package com.fksoft.domain.booking;

import com.fksoft.domain.payment.PaymentFailed;
import com.fksoft.domain.payment.PaymentSucceeded;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges payment settlement events to the reservation lifecycle (SPEC-0016). Reacts AFTER_COMMIT of
 * the webhook transaction; {@link ReservationConfirmer} applies the change in its own transaction.
 */
@Component
class PaymentEventListener {

    private final ReservationConfirmer confirmer;

    PaymentEventListener(ReservationConfirmer confirmer) {
        this.confirmer = confirmer;
    }

    @TransactionalEventListener
    void on(PaymentSucceeded event) {
        confirmer.onPaymentSucceeded(event.reservationId(), event.amountCents());
    }

    @TransactionalEventListener
    void on(PaymentFailed event) {
        confirmer.onPaymentFailed(event.reservationId());
    }
}
