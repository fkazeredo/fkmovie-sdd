# 0017 - Reservation Expiration

Status: Draft
Related ADRs: 0002, 0004

## Goal

Stale holds release their seats automatically: `PENDING` reservations past
`expiresAt` become `EXPIRED`; `AWAITING_PAYMENT` reservations past
`payment_deadline_at` become `CANCELLED`. Seats return to `FREE` and the
seat map updates live.

## Scope

`booking` module scheduled job. Two sweeps in one job run. Late-payment
race handling is defined in 0015/0016 (refund on late success).

## Business Rules

- Sweep 1: `status = PENDING AND expires_at <= now` →
  `EXPIRED`; seats `HELD → FREE`.
- Sweep 2: `status = AWAITING_PAYMENT AND payment_deadline_at <= now` →
  `CANCELLED`; seats `HELD → FREE`. (If the payment later succeeds, 0016's
  late-success rule refunds automatically.)
- `CONFIRMED` reservations are never expired; `SOLD` seats are never
  released by this job.
- The job runs every 30 s (`app.booking.expiration-interval`), single
  instance (ADR 0002 — no distributed lock).
- Each reservation is processed in its own transaction (batch of max 100
  per run, `app.booking.expiration-batch-size`): one poisoned row must not
  block the sweep. Rows are claimed `FOR UPDATE SKIP LOCKED` to coexist
  with in-flight confirmations (ADR 0004).
- Idempotent and safe to run repeatedly: state-machine guards make a second
  pass a no-op.
- Time comes from an injected `java.time.Clock` (testability).
- After each reservation's commit: realtime `SeatsStatusChanged(FREE)` on
  the screening topic and `RESERVATION_STATUS_CHANGED` on the owner queue.
- No expiration email in v1 (noise); metric + audit only.

## API Contracts

None (internal job).

## Events

Publishes internal `ReservationExpired(reservationId, userId)` and
`ReservationCancelled(reservationId, userId, reason=PAYMENT_TIMEOUT)`;
realtime events as above.

## Persistence Changes

None new — uses `reservations(status, expires_at)` index from 0014; add
index `(status, payment_deadline_at)`.

## Validation Rules

- Domain transition guards (`expire()` only from PENDING;
  `cancelForPaymentTimeout()` only from AWAITING_PAYMENT).

## Error Behavior

Job errors are logged with reservationId and continue with the next row.
A row failing 3 consecutive runs raises an ERROR log (operational alert).

## Observability Requirements

- Metrics: `reservations_expired_total`,
  `reservations_payment_timeout_total`, `expiration_job_duration_seconds`,
  `expiration_job_batch_size` (histogram), `expiration_job_errors_total`.
- Log each run summary: scanned, expired, cancelled, errors.

## Tests Required

- Integration with mutable test `Clock`: advance past `expiresAt` → run →
  EXPIRED + seats FREE + realtime sent.
- Integration: AWAITING_PAYMENT past deadline → CANCELLED + FREE.
- Integration: CONFIRMED untouched; SOLD never released; double run no-op.
- Integration: SKIP LOCKED — a row being confirmed concurrently is skipped,
  not blocked.

## Acceptance Criteria

- A hold visibly returns to FREE on another browser within ~35 s of
  expiry in dev.

## Open Questions

- 30 s interval and batch 100 — confirm defaults.

## Out of Scope

- Manual cancellation (0018). Multi-instance locking (ADR 0002 revision).
  Notification emails on expiry.
