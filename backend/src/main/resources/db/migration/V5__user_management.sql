-- Admin-managed users via invitation (SPEC-0005). Invited users exist with no password
-- until they accept, so password_hash becomes nullable; provenance is recorded on the row.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN invited_by_user_id UUID REFERENCES users (id);
ALTER TABLE users ADD COLUMN invited_at TIMESTAMPTZ;

CREATE TABLE invitation_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_invitation_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_invitation_tokens_user_id ON invitation_tokens (user_id);
