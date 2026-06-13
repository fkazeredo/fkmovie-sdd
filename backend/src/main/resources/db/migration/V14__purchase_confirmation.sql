-- Tickets issued on purchase confirmation (SPEC-0016). One ticket per reservation seat, with a
-- globally unique code FKM-YYYY-NNNNNN from a single sequence (the year is informational; no annual
-- reset, avoiding reset-locking complexity). reservations.payment_id remembers the charge so a
-- re-confirm is idempotent (same payment, no second charge).
CREATE SEQUENCE ticket_code_seq;

CREATE TABLE tickets (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(64) NOT NULL DEFAULT 'default',
    reservation_seat_id UUID        NOT NULL REFERENCES reservation_seats (id),
    code                VARCHAR(20) NOT NULL,
    status              VARCHAR(10) NOT NULL CHECK (status IN ('VALID', 'CANCELLED')),
    issued_at           TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_tickets_reservation_seat UNIQUE (reservation_seat_id),
    CONSTRAINT uq_tickets_code UNIQUE (code)
);

ALTER TABLE reservations ADD COLUMN payment_id UUID;
