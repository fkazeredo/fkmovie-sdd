package com.fksoft.domain.notification;

/**
 * Failure to deliver an email. {@code transientFailure} drives the outbox retry decision:
 * transient errors (timeout, connection, rate limit) are retried with backoff; permanent
 * errors (invalid recipient, unrecoverable 5xx) go straight to the dead-letter status.
 */
public class EmailSendException extends RuntimeException {

    private final boolean transientFailure;

    public EmailSendException(String message, boolean transientFailure, Throwable cause) {
        super(message, cause);
        this.transientFailure = transientFailure;
    }

    public boolean isTransient() {
        return transientFailure;
    }
}
