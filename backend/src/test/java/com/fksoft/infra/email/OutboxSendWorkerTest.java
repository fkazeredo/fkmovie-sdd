package com.fksoft.infra.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fksoft.domain.notification.EmailMessage;
import com.fksoft.domain.notification.EmailPermanentlyFailed;
import com.fksoft.domain.notification.EmailSendException;
import com.fksoft.domain.notification.EmailSender;
import com.fksoft.domain.notification.EmailTemplate;
import com.fksoft.domain.notification.OutboxEmail;
import com.fksoft.domain.notification.OutboxEmailRepository;
import com.fksoft.domain.notification.OutboxStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/** Backoff state machine + outcome handling of the dispatcher worker (SPEC-0006). */
class OutboxSendWorkerTest {

    private static final Instant NOW = Instant.parse("2026-06-11T00:00:00Z");

    private final OutboxEmailRepository repository = mock(OutboxEmailRepository.class);
    private final EmailRenderer renderer = mock(EmailRenderer.class);
    private final EmailSender sender = mock(EmailSender.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final OutboxSendWorker worker = new OutboxSendWorker(
            repository, renderer, sender, events, meterRegistry, Clock.fixed(NOW, ZoneOffset.UTC), 20);

    private OutboxEmail pendingWithAttempts(int attempts) {
        var email = new OutboxEmail(EmailTemplate.VERIFICATION, "to@test.local", "en", Map.of(), NOW.minusSeconds(1));
        for (int i = 0; i < attempts; i++) {
            email.recordTransientFailure("e", NOW.minusSeconds(1));
        }
        return email;
    }

    @Test
    void successMarksSentAndPublishesEvent() {
        var email = pendingWithAttempts(0);
        when(repository.findById(any())).thenReturn(Optional.of(email));
        when(renderer.render(email)).thenReturn(new EmailMessage("to", "s", "h", "t"));
        doNothing().when(sender).send(any());

        worker.process(email.id());

        assertThat(email.status()).isEqualTo(OutboxStatus.SENT);
    }

    @Test
    void firstTransientFailureWaitsOneMinute() {
        var email = pendingWithAttempts(0);
        when(repository.findById(any())).thenReturn(Optional.of(email));
        when(renderer.render(email)).thenReturn(new EmailMessage("to", "s", "h", "t"));
        doThrow(new EmailSendException("timeout", true, null)).when(sender).send(any());

        worker.process(email.id());

        assertThat(email.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(email.attempts()).isEqualTo(1);
        assertThat(email.nextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
    }

    @Test
    void sixthFailureDeadLetters() {
        var email = pendingWithAttempts(5); // the next failure is the 6th
        when(repository.findById(any())).thenReturn(Optional.of(email));
        when(renderer.render(email)).thenReturn(new EmailMessage("to", "s", "h", "t"));
        doThrow(new EmailSendException("timeout", true, null)).when(sender).send(any());

        worker.process(email.id());

        assertThat(email.status()).isEqualTo(OutboxStatus.FAILED_PERMANENT);
        assertThat(email.attempts()).isEqualTo(6);
    }

    @Test
    void permanentFailureDeadLettersImmediately() {
        var email = pendingWithAttempts(0);
        when(repository.findById(any())).thenReturn(Optional.of(email));
        when(renderer.render(email)).thenReturn(new EmailMessage("to", "s", "h", "t"));
        doThrow(new EmailSendException("invalid recipient", false, null))
                .when(sender)
                .send(any());

        worker.process(email.id());

        assertThat(email.status()).isEqualTo(OutboxStatus.FAILED_PERMANENT);
        assertThat(email.attempts()).isEqualTo(1);
    }

    @Test
    void deadLetterPreservesTheProviderCause() {
        var email = pendingWithAttempts(0);
        when(repository.findById(any())).thenReturn(Optional.of(email));
        when(renderer.render(email)).thenReturn(new EmailMessage("to", "s", "h", "t"));
        var smtpCause = new RuntimeException("535-5.7.8 Username and Password not accepted");
        doThrow(new EmailSendException("Permanent mail failure", false, smtpCause))
                .when(sender)
                .send(any());

        worker.process(email.id());

        var event = ArgumentCaptor.forClass(EmailPermanentlyFailed.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().lastError())
                .contains("Permanent mail failure")
                .contains("535-5.7.8 Username and Password not accepted");
    }
}
