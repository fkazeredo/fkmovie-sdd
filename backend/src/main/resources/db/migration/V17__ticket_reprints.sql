-- Audited operator ticket reprints (SPEC-0020): one row per reprint of a ticket by an operator. The
-- reprint count is informational (fraud awareness); no hard limit and no ticket-state change.
CREATE TABLE ticket_reprints (
    id               UUID PRIMARY KEY,
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default',
    ticket_id        UUID        NOT NULL REFERENCES tickets (id),
    operator_user_id UUID        NOT NULL REFERENCES users (id),
    reprinted_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ticket_reprints_ticket_id ON ticket_reprints (ticket_id);
