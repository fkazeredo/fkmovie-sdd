-- Pricing configuration (SPEC-0012): admin-editable seat-type surcharges and weekday
-- multipliers. The price formula is full = round_half_up((base + surcharge) * multiplier);
-- half = ceil(full / 2). Seeds are essential system data. ACCESSIBLE/COMPANION surcharge must
-- stay 0 (legal sensitivity — enforced by the application). day_of_week is ISO Mon=1..Sun=7.
CREATE TABLE pricing_seat_type (
    seat_type      VARCHAR(20) PRIMARY KEY CHECK (seat_type IN ('STANDARD', 'VIP', 'ACCESSIBLE', 'COMPANION')),
    surcharge_cents INT        NOT NULL CHECK (surcharge_cents >= 0)
);

INSERT INTO pricing_seat_type (seat_type, surcharge_cents) VALUES
    ('STANDARD', 0),
    ('VIP', 1000),
    ('ACCESSIBLE', 0),
    ('COMPANION', 0);

CREATE TABLE pricing_weekday (
    day_of_week INT          PRIMARY KEY CHECK (day_of_week BETWEEN 1 AND 7),
    multiplier  NUMERIC(3, 2) NOT NULL CHECK (multiplier BETWEEN 0.10 AND 2.00)
);

INSERT INTO pricing_weekday (day_of_week, multiplier) VALUES
    (1, 1.00), (2, 1.00), (3, 1.00), (4, 1.00), (5, 1.00), (6, 1.00), (7, 1.00);
