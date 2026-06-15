package com.fksoft.domain.notification;

import java.time.Instant;
import java.util.UUID;

/** Public event: an outbox email reached the dead-letter status (SPEC-0006). */
public record EmailPermanentlyFailed(
        UUID emailId, String recipient, EmailTemplate template, String lastError, Instant occurredAt) {}
