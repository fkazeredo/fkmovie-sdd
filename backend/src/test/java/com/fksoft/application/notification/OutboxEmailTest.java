package com.fksoft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Outbox row state transitions (SPEC-0006). */
class OutboxEmailTest {

    private OutboxEmail pending() {
        return new OutboxEmail(EmailTemplate.VERIFICATION, "to@test.local", "pt-BR", Map.of("k", "v"), Instant.EPOCH);
    }

    @Test
    void startsPendingDueImmediately() {
        var email = pending();
        assertThat(email.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(email.attempts()).isZero();
        assertThat(email.nextAttemptAt()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void markSentTransitionsToSent() {
        var email = pending();
        email.markSent(Instant.EPOCH.plusSeconds(5));
        assertThat(email.status()).isEqualTo(OutboxStatus.SENT);
    }

    @Test
    void transientFailureIncrementsAttemptsAndReschedules() {
        var email = pending();
        var next = Instant.EPOCH.plusSeconds(60);
        email.recordTransientFailure("smtp timeout", next);
        assertThat(email.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(email.attempts()).isEqualTo(1);
        assertThat(email.nextAttemptAt()).isEqualTo(next);
    }

    @Test
    void permanentFailureDeadLetters() {
        var email = pending();
        email.markPermanentlyFailed("invalid recipient");
        assertThat(email.status()).isEqualTo(OutboxStatus.FAILED_PERMANENT);
        assertThat(email.attempts()).isEqualTo(1);
    }
}
