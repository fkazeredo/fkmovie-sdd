-- Physical cinema structure (SPEC-0007, ADR 0001, ADR 0003): rooms and typed seats.
-- Rooms are seed-managed in v1 (no REST CRUD); a physical seat has NO availability status —
-- availability is per-screening and belongs to ScreeningSeat (SPEC-0011).
-- Column names seat_row/seat_number avoid the reserved word "row" in Postgres; the JPA
-- entity maps its row/number fields onto these columns.
CREATE TABLE cinema_rooms (
    id         UUID PRIMARY KEY,
    tenant_id  VARCHAR(64)  NOT NULL DEFAULT 'default',
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_cinema_rooms_name UNIQUE (name)
);
CREATE INDEX idx_cinema_rooms_tenant_id ON cinema_rooms (tenant_id);

CREATE TABLE seats (
    id          UUID PRIMARY KEY,
    room_id     UUID        NOT NULL REFERENCES cinema_rooms (id),
    seat_row    VARCHAR(2)  NOT NULL,
    seat_number INT         NOT NULL,
    type        VARCHAR(20) NOT NULL CHECK (type IN ('STANDARD', 'VIP', 'ACCESSIBLE', 'COMPANION')),
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_seats_room_row_number UNIQUE (room_id, seat_row, seat_number)
);
CREATE INDEX idx_seats_room_id ON seats (room_id);
