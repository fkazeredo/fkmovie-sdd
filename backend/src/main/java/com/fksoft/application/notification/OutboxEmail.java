package com.fksoft.application.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An email awaiting delivery (SPEC-0006). The render payload is stored as JSONB; the row
 * transitions PENDING → SENT on success, stays PENDING with a later {@code nextAttemptAt}
 * on a transient failure, and reaches FAILED_PERMANENT after a permanent error or the 6th
 * failed attempt.
 */
@Entity
@Table(name = "outbox_emails")
public class OutboxEmail {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_key", nullable = false)
    private EmailTemplate templateKey;

    @Column(nullable = false)
    private String locale;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false)
    private Map<String, String> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboxEmail() {
        // JPA
    }

    /** Creates a PENDING email due immediately. */
    public OutboxEmail(
            EmailTemplate templateKey, String recipientEmail, String locale, Map<String, String> payload, Instant now) {
        this.id = UUID.randomUUID();
        this.tenantId = "default";
        this.templateKey = templateKey;
        this.recipientEmail = recipientEmail;
        this.locale = locale;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public void markSent(Instant now) {
        this.status = OutboxStatus.SENT;
        this.sentAt = now;
        this.lastError = null;
    }

    /** Records a transient failure and schedules the next attempt. */
    public void recordTransientFailure(String error, Instant nextAttemptAt) {
        this.attempts++;
        this.lastError = error;
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markPermanentlyFailed(String error) {
        this.attempts++;
        this.status = OutboxStatus.FAILED_PERMANENT;
        this.lastError = error;
    }

    public UUID id() {
        return id;
    }

    public String recipientEmail() {
        return recipientEmail;
    }

    public EmailTemplate templateKey() {
        return templateKey;
    }

    public String locale() {
        return locale;
    }

    public Map<String, String> payload() {
        return payload;
    }

    public OutboxStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public Instant nextAttemptAt() {
        return nextAttemptAt;
    }
}
