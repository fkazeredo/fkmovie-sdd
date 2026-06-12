-- Movie catalog (SPEC-0008, ADR 0001). Admin-managed; feeds screenings (0009) and the public
-- list (0010). Title is NOT unique: re-releases exist (duplicates are a UI warning, not an error).
CREATE TABLE movies (
    id               UUID PRIMARY KEY,
    tenant_id        VARCHAR(64)   NOT NULL DEFAULT 'default',
    title            VARCHAR(200)  NOT NULL,
    duration_minutes INT           NOT NULL CHECK (duration_minutes BETWEEN 1 AND 600),
    synopsis         VARCHAR(2000),
    poster_url       VARCHAR(2048),
    age_rating       VARCHAR(3)    NOT NULL CHECK (age_rating IN ('L', 'A10', 'A12', 'A14', 'A16', 'A18')),
    status           VARCHAR(20)   NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_movies_tenant_status ON movies (tenant_id, status);
-- Case-insensitive title search (LOWER(title) LIKE ...); trigram is deferred (single-cinema scale).
CREATE INDEX idx_movies_lower_title ON movies (LOWER(title));
