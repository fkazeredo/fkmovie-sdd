-- Drives the payment-timeout sweep of the expiration job (SPEC-0017): claim AWAITING_PAYMENT
-- reservations past their payment_deadline_at. The PENDING/expires_at sweep already has its index
-- (idx_reservations_status_expires_at, created in V12).
CREATE INDEX idx_reservations_status_payment_deadline_at
    ON reservations (status, payment_deadline_at);
