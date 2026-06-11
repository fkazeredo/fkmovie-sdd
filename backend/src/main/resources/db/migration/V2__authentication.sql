-- Authentication schema (SPEC-0003, ADR 0005, ADR 0003).
-- citext gives DB-level case-insensitive uniqueness for users.email; the application
-- additionally normalizes emails to lowercase at storage (defense in depth, see spec note).
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE users (
    id                UUID PRIMARY KEY,
    tenant_id         VARCHAR(64)  NOT NULL DEFAULT 'default',
    email             CITEXT       NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    name              VARCHAR(150) NOT NULL,
    role              VARCHAR(20)  NOT NULL CHECK (role IN ('CUSTOMER', 'OPERATOR', 'ADMIN')),
    status            VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    email_verified_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE INDEX idx_users_tenant_id ON users (tenant_id);

CREATE TABLE refresh_tokens (
    id             UUID PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES users (id),
    token_hash     VARCHAR(64) NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,
    replaced_by_id UUID REFERENCES refresh_tokens (id),
    created_at     TIMESTAMPTZ NOT NULL,
    ip             VARCHAR(45),
    user_agent     VARCHAR(400),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- Append-only audit/rate-limit trail; identity PK over UUID for cheap inserts.
-- Email is stored lowercase by the application. Retention job: open question in SPEC-0003.
CREATE TABLE login_attempts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    ip           VARCHAR(45)  NOT NULL,
    succeeded    BOOLEAN      NOT NULL,
    attempted_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_login_attempts_email_attempted_at ON login_attempts (email, attempted_at);
CREATE INDEX idx_login_attempts_ip_attempted_at ON login_attempts (ip, attempted_at);
