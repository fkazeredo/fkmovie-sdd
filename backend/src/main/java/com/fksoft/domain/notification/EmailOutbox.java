package com.fksoft.domain.notification;

import java.time.Clock;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enqueues emails into the transactional outbox (SPEC-0006). This is the seam the
 * notification listeners call after a source transaction commits; the dispatcher does the
 * actual sending. Enqueue is the public contract, send is implementation.
 */
@Service
@RequiredArgsConstructor
class EmailOutbox {

    private final OutboxEmailRepository repository;
    private final Clock clock;

    /**
     * Persists a PENDING email due immediately. REQUIRES_NEW because the only caller is an
     * AFTER_COMMIT listener: a REQUIRED transaction there would join the already-committed
     * source transaction and the insert would never be flushed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void enqueue(EmailTemplate template, String recipient, String locale, Map<String, String> payload) {
        repository.save(new OutboxEmail(template, recipient, locale, payload, clock.instant()));
    }
}
