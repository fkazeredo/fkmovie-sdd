package com.fksoft.domain.payment;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public read API of the payment ledger (SPEC-0019): the synchronous collaboration point booking uses
 * to show a reservation's refund summary. Returns a stable {@link RefundView}, never the {@code
 * Payment} entity, and never touches reservations.
 */
@Service
@RequiredArgsConstructor
public class PaymentLedger {

    private final PaymentRepository payments;

    /** The latest refund recorded for a reservation, if any (SPEC-0019). */
    @Transactional(readOnly = true)
    public Optional<RefundView> latestRefund(UUID reservationId) {
        return payments.findFirstByReservationIdAndKindOrderByCreatedAtDesc(reservationId, PaymentKind.REFUND)
                .map(payment -> new RefundView(payment.amountCents(), payment.status()));
    }
}
