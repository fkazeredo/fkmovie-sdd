-- Backs the owner-scoped "my reservations" listing (SPEC-0019): newest first per user.
CREATE INDEX idx_reservations_user_created_at
    ON reservations (user_id, created_at DESC);
