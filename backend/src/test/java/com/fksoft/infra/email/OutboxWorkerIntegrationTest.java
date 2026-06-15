package com.fksoft.infra.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.AbstractIntegrationTest;
import com.fksoft.domain.notification.EmailTemplate;
import com.fksoft.domain.notification.OutboxEmail;
import com.fksoft.domain.notification.OutboxEmailRepository;
import com.fksoft.domain.notification.OutboxStatus;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Worker against a real database with no SMTP server listening: the send fails transiently
 * (connection refused) so the row stays PENDING with one attempt and a future next-attempt
 * time (SPEC-0006 retry path). The happy path runs in {@link EmailDeliveryGreenMailTest}.
 */
class OutboxWorkerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxEmailRepository outboxEmails;

    @Autowired
    private OutboxSendWorker worker;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clean() {
        jdbcTemplate.update("DELETE FROM outbox_emails");
    }

    @Test
    void claimsDueRowAndRetriesOnTransientFailure() {
        var due = new OutboxEmail(
                EmailTemplate.VERIFICATION,
                "frank@test.local",
                "en",
                Map.of("name", "Frank", "link", "http://localhost:4200/verify-email?token=abc"),
                Instant.now().minusSeconds(1));
        outboxEmails.save(due);

        var claimed = worker.claimDueIds();
        assertThat(claimed).contains(due.id());

        worker.process(due.id());

        var after = outboxEmails.findById(due.id()).orElseThrow();
        assertThat(after.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(after.attempts()).isEqualTo(1);
        assertThat(after.nextAttemptAt()).isAfter(Instant.now());
    }
}
