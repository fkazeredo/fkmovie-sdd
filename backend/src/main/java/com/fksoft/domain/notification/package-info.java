/**
 * Notification module (SPEC-0006, ADR 0007): reliable asynchronous email delivery via a
 * transactional outbox. Other modules publish domain events; this module reacts with
 * {@code @TransactionalEventListener(AFTER_COMMIT)} and enqueues an email. A scheduled
 * dispatcher sends with exponential-backoff retry and a dead-letter status.
 *
 * <p>Public API: the {@code EmailSent} / {@code EmailPermanentlyFailed} events. The
 * {@code outbox_emails} table and the {@code EmailSender} port are module-internal.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Notification")
package com.fksoft.domain.notification;
