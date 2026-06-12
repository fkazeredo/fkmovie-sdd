-- Temporary seat reservations (SPEC-0014, ADR 0004). A verified customer holds FREE seats for a
-- screening; prices are snapshotted on reservation_seats. Concurrency safety: the FREE→HELD
-- transition uses a pessimistic row lock on screening_seats; the partial unique index below caps
-- a user to one active reservation per screening; reservations carry an optimistic version.
CREATE TABLE reservations (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(64) NOT NULL DEFAULT 'default',
    user_id             UUID        NOT NULL REFERENCES users (id),
    screening_id        UUID        NOT NULL REFERENCES screenings (id),
    status              VARCHAR(20) NOT NULL CHECK (status IN
                            ('PENDING', 'AWAITING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    total_cents         INT         NOT NULL CHECK (total_cents >= 0),
    expires_at          TIMESTAMPTZ NOT NULL,
    payment_deadline_at TIMESTAMPTZ,
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);

-- At most one active (held / awaiting payment) reservation per user per screening (SPEC-0014).
CREATE UNIQUE INDEX uq_active_reservation
    ON reservations (user_id, screening_id)
    WHERE status IN ('PENDING', 'AWAITING_PAYMENT');

-- Drives the expiration job (SPEC-0017).
CREATE INDEX idx_reservations_status_expires_at ON reservations (status, expires_at);

CREATE TABLE reservation_seats (
    id                  UUID PRIMARY KEY,
    reservation_id      UUID        NOT NULL REFERENCES reservations (id),
    screening_seat_id   UUID        NOT NULL REFERENCES screening_seats (id),
    ticket_type         VARCHAR(10) NOT NULL CHECK (ticket_type IN ('FULL', 'HALF')),
    half_price_category VARCHAR(20) CHECK (half_price_category IN
                            ('STUDENT', 'ELDERLY', 'PCD', 'PCD_COMPANION', 'LOW_INCOME_YOUTH')),
    document_reference  VARCHAR(60),
    price_cents         INT         NOT NULL CHECK (price_cents > 0),
    CONSTRAINT uq_reservation_seat UNIQUE (reservation_id, screening_seat_id)
);
CREATE INDEX idx_reservation_seats_reservation_id ON reservation_seats (reservation_id);
