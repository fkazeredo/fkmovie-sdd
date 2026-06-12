-- Screenings: a movie scheduled in a room at a time (SPEC-0009, ADR 0009).
-- ends_at is derived (starts_at + movie.duration + cleaning buffer) and stored.
-- Room overlap is guaranteed by a GiST exclusion constraint over the time range; the
-- application also pre-checks to return a friendly error. btree_gist lets the equality on
-- room_id and the range overlap share one GiST index.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE screenings (
    id               UUID PRIMARY KEY,
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default',
    movie_id         UUID        NOT NULL REFERENCES movies (id),
    room_id          UUID        NOT NULL REFERENCES cinema_rooms (id),
    starts_at        TIMESTAMPTZ NOT NULL,
    ends_at          TIMESTAMPTZ NOT NULL,
    base_price_cents INT         NOT NULL CHECK (base_price_cents > 0),
    status           VARCHAR(20) NOT NULL CHECK (status IN ('SCHEDULED', 'CANCELLED')),
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT excl_screenings_room_overlap
        EXCLUDE USING gist (room_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
        WHERE (status = 'SCHEDULED')
);
CREATE INDEX idx_screenings_room_starts_at ON screenings (room_id, starts_at);
CREATE INDEX idx_screenings_movie_id ON screenings (movie_id);
