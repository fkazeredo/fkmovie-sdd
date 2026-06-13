package com.fksoft.application.payment;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Idempotency ledger for processed webhooks (SPEC-0015). Module-internal. */
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

    boolean existsByPaymentIdAndEventType(UUID paymentId, String eventType);
}
