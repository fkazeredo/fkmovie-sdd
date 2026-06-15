package com.fksoft.domain.notification;

import java.time.Instant;
import java.util.UUID;

/** Public event: an outbox email was delivered (SPEC-0006). */
public record EmailSent(UUID emailId, String recipient, EmailTemplate template, Instant occurredAt) {}
