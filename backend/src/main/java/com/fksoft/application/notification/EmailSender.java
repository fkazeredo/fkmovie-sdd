package com.fksoft.application.notification;

/**
 * Port for the actual email transport (ADR 0007). The single Spring Mail implementation is
 * swapped per environment by configuration, never by code. Internal to the notification
 * module — other modules trigger email by publishing domain events, not by calling this.
 */
public interface EmailSender {

    /**
     * Sends a rendered message.
     *
     * @throws EmailSendException classifying the failure as transient (retryable) or permanent.
     */
    void send(EmailMessage message);
}
