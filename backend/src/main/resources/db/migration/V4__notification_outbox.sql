-- Transactional outbox for asynchronous email delivery (SPEC-0006, ADR 0007). The worker
-- polls PENDING rows whose next_attempt_at is due; retries use exponential backoff and a
-- FAILED_PERMANENT dead-letter status. Cleanup of old rows: deferred (see SPEC-0006).
CREATE TABLE outbox_emails (
    id              UUID PRIMARY KEY,
    tenant_id       VARCHAR(64)  NOT NULL DEFAULT 'default',
    recipient_email VARCHAR(255) NOT NULL,
    template_key    VARCHAR(50)  NOT NULL,
    locale          VARCHAR(10)  NOT NULL,
    payload_json    JSONB        NOT NULL,
    status          VARCHAR(20)  NOT NULL
                    CHECK (status IN ('PENDING', 'SENT', 'FAILED_PERMANENT')),
    attempts        INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL,
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    sent_at         TIMESTAMPTZ
);
CREATE INDEX idx_outbox_emails_status_next_attempt ON outbox_emails (status, next_attempt_at);
CREATE INDEX idx_outbox_emails_created_at ON outbox_emails (created_at);
