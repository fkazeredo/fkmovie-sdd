-- Per-screening seat inventory (SPEC-0009 materializes it; SPEC-0011 owns the read model).
-- The booking module creates one row per physical seat of the screening's room, all FREE.
-- UNIQUE(screening_id, seat_id) makes the event-driven materialization idempotent and is the
-- first defense against double booking (ADR 0004). version backs optimistic locking (0014).
CREATE TABLE screening_seats (
    id           UUID PRIMARY KEY,
    tenant_id    VARCHAR(64) NOT NULL DEFAULT 'default',
    screening_id UUID        NOT NULL REFERENCES screenings (id),
    seat_id      UUID        NOT NULL REFERENCES seats (id),
    status       VARCHAR(20) NOT NULL CHECK (status IN ('FREE', 'HELD', 'SOLD')),
    version      BIGINT      NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_screening_seats UNIQUE (screening_id, seat_id)
);
CREATE INDEX idx_screening_seats_screening_status ON screening_seats (screening_id, status);
