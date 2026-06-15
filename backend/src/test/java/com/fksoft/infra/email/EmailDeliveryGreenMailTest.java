package com.fksoft.infra.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.AbstractIntegrationTest;
import com.fksoft.domain.notification.EmailTemplate;
import com.fksoft.domain.notification.OutboxEmail;
import com.fksoft.domain.notification.OutboxEmailRepository;
import com.fksoft.domain.notification.OutboxStatus;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * End-to-end SMTP path (SPEC-0006): the worker renders a real template and the real Spring
 * Mail sender delivers it to a GreenMail server on the configured port (3025).
 */
class EmailDeliveryGreenMailTest extends AbstractIntegrationTest {

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP);

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
    void rendersAndDeliversAVerificationEmail() throws Exception {
        var email = new OutboxEmail(
                EmailTemplate.VERIFICATION,
                "frank@test.local",
                "pt-BR",
                Map.of("name", "Frank", "link", "http://localhost:4200/verify-email?token=abc"),
                Instant.now());
        outboxEmails.save(email);

        worker.process(email.id());

        assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
        var received = GREEN_MAIL.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("Confirme seu e-mail no fkmovies");
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("frank@test.local");
        assertThat(outboxEmails.findById(email.id()).orElseThrow().status()).isEqualTo(OutboxStatus.SENT);
    }
}
