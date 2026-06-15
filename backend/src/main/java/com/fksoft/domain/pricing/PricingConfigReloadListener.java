package com.fksoft.domain.pricing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Refreshes the in-memory pricing snapshot after an admin change commits (SPEC-0012). AFTER_COMMIT
 * so the reload reads the persisted values; the next quote sees the new config.
 */
@Component
@RequiredArgsConstructor
class PricingConfigReloadListener {

    private final PricingConfig config;

    @TransactionalEventListener
    void on(PricingConfigChanged event) {
        config.reload();
    }
}
