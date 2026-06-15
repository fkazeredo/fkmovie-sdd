package com.fksoft.infra.email;

import com.fksoft.domain.notification.EmailPermanentlyFailed;
import com.fksoft.domain.notification.EmailSendException;
import com.fksoft.domain.notification.EmailSender;
import com.fksoft.domain.notification.EmailSent;
import com.fksoft.domain.notification.OutboxEmail;
import com.fksoft.domain.notification.OutboxEmailRepository;
import com.fksoft.domain.notification.OutboxStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional unit of work behind the outbox dispatcher (SPEC-0006). Claiming and
 * per-email processing each run in their own transaction so one failed send never rolls
 * back its siblings, and a crash mid-batch leaves every untouched row PENDING for the next
 * poll (safe restart).
 */
@Component
@Slf4j
class OutboxSendWorker {

    /** Backoff before each retry; the 6th failure dead-letters instead of waiting again. */
    static final Duration[] BACKOFF = {
        Duration.ofMinutes(1),
        Duration.ofMinutes(5),
        Duration.ofMinutes(15),
        Duration.ofHours(1),
        Duration.ofHours(6),
        Duration.ofHours(24)
    };

    static final int MAX_FAILURES = 6;

    private final OutboxEmailRepository repository;
    private final EmailRenderer renderer;
    private final EmailSender emailSender;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final int batchSize;

    OutboxSendWorker(
            OutboxEmailRepository repository,
            EmailRenderer renderer,
            EmailSender emailSender,
            ApplicationEventPublisher events,
            MeterRegistry meterRegistry,
            Clock clock,
            @Value("${app.notification.batch-size}") int batchSize) {
        this.repository = repository;
        this.renderer = renderer;
        this.emailSender = emailSender;
        this.events = events;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    List<UUID> claimDueIds() {
        return repository.claimDue(clock.instant(), PageRequest.of(0, batchSize)).stream()
                .map(OutboxEmail::id)
                .toList();
    }

    /** Renders and sends one email, transitioning its state by the send outcome. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void process(UUID id) {
        var email = repository.findById(id).orElse(null);
        if (email == null || email.status() != OutboxStatus.PENDING) {
            return;
        }
        var sample = Timer.start(meterRegistry);
        try {
            emailSender.send(renderer.render(email));
            email.markSent(clock.instant());
            recordOutcome(sample, "success");
            events.publishEvent(
                    new EmailSent(email.id(), email.recipientEmail(), email.templateKey(), clock.instant()));
        } catch (EmailSendException e) {
            handleFailure(email, e);
            recordOutcome(sample, e.isTransient() ? "transient_failure" : "permanent_failure");
        }
    }

    private void handleFailure(OutboxEmail email, EmailSendException failure) {
        var error = describe(failure);
        int failureNumber = email.attempts() + 1;
        if (!failure.isTransient() || failureNumber >= MAX_FAILURES) {
            email.markPermanentlyFailed(error);
            meterRegistry
                    .counter(
                            "email.permanent_failures",
                            "template",
                            email.templateKey().name())
                    .increment();
            events.publishEvent(new EmailPermanentlyFailed(
                    email.id(), email.recipientEmail(), email.templateKey(), error, clock.instant()));
            // Log the full failure (with its provider cause) so a dead-letter is diagnosable.
            log.warn(
                    "email dead-lettered id={} template={} recipient={}: {}",
                    email.id(),
                    email.templateKey(),
                    email.recipientEmail(),
                    error,
                    failure);
        } else {
            email.recordTransientFailure(error, clock.instant().plus(BACKOFF[failureNumber - 1]));
            log.warn("email retry scheduled id={} attempt={}: {}", email.id(), failureNumber, error, failure);
        }
    }

    /**
     * Failure message plus the root provider cause (e.g. the SMTP server's reply), so the stored
     * error and the dead-letter log reveal exactly why mail failed instead of a generic message.
     */
    private static String describe(EmailSendException failure) {
        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root == failure || root.getMessage() == null
                ? failure.getMessage()
                : failure.getMessage() + ": " + root.getMessage();
    }

    private void recordOutcome(Timer.Sample sample, String outcome) {
        sample.stop(meterRegistry.timer("email.send.latency", "outcome", outcome));
        meterRegistry.counter("email.send.attempts", "outcome", outcome).increment();
    }
}
