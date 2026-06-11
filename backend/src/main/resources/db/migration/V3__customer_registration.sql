-- Customer self-registration: per-user locale plus single-use, hashed tokens for email
-- verification (24h) and password reset (1h). SPEC-0004.
ALTER TABLE users ADD COLUMN preferred_locale VARCHAR(10) NOT NULL DEFAULT 'pt-BR';

CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_email_verification_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens (user_id);

CREATE TABLE password_reset_tokens (
    id           UUID PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users (id),
    token_hash   VARCHAR(64) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    consumed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL,
    requested_ip VARCHAR(45),
    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
