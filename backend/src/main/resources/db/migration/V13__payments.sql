-- Payment ledger and the mock gateway's delivery queue (SPEC-0015, ADR 0006). The mock persists a
-- job scheduled for now+delay; a worker then POSTs a signed webhook to the app's own endpoint.
-- payment_webhook_events is the idempotency ledger: a (payment_id, event_type) is processed once.
CREATE TABLE payments (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(64) NOT NULL DEFAULT 'default',
    -- Soft reference (no FK): keeps the payment ledger decoupled from the booking module's table
    -- (ADR 0006 boundary); the application guarantees the reservation exists.
    reservation_id      UUID        NOT NULL,
    kind                VARCHAR(10) NOT NULL CHECK (kind IN ('CHARGE', 'REFUND')),
    status              VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    amount_cents        INT         NOT NULL CHECK (amount_cents > 0),
    provider            VARCHAR(20) NOT NULL DEFAULT 'MOCK',
    provider_payment_id VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL,
    settled_at          TIMESTAMPTZ
);
CREATE INDEX idx_payments_reservation_id ON payments (reservation_id);

CREATE TABLE payment_webhook_events (
    id          UUID PRIMARY KEY,
    payment_id  UUID        NOT NULL REFERENCES payments (id),
    event_type  VARCHAR(40) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_webhook UNIQUE (payment_id, event_type)
);

CREATE TABLE mock_payment_jobs (
    id           UUID PRIMARY KEY,
    payment_id   UUID        NOT NULL REFERENCES payments (id),
    deliver_at   TIMESTAMPTZ NOT NULL,
    outcome      VARCHAR(10) NOT NULL CHECK (outcome IN ('SUCCEEDED', 'FAILED')),
    delivered_at TIMESTAMPTZ
);
CREATE INDEX idx_mock_payment_jobs_due ON mock_payment_jobs (delivered_at, deliver_at);
