# 0016 - Purchase Confirmation

Status: Implemented
Related ADRs: 0006, 0004, 0009

> Implementation notes (backend):
> - Open Question resolved: payment deadline 10 min (`app.booking.payment-deadline-minutes`).
> - Confirm (owner only) is idempotent on AWAITING_PAYMENT (same paymentId, no second charge);
>   stores `reservations.payment_id`. Expiry checked against the injected `Clock` → 410 even if
>   unswept.
> - On `PaymentSucceeded` (after-commit, REQUIRES_NEW): CONFIRMED, seats HELD→SOLD, one VALID ticket
>   per seat (FKM-YYYY-NNNNNN from `ticket_code_seq`), `ReservationConfirmed` → ticket email. Late
>   success (non-AWAITING_PAYMENT) → automatic refund + WARN, no state change. On `PaymentFailed`:
>   CANCELLED, seats HELD→FREE.
> - Error codes match the table: `booking.not-owner` (403), `booking.already-confirmed` (409),
>   `booking.reservation-expired` (410), `booking.reservation-cancelled` (409). A non-customer hitting
>   the endpoint gets the transport-level 403 `auth.forbidden` (security chain), distinct from the
>   business `booking.not-owner`.
> - `auth.AccountView` was extended with email/name/locale so the confirmation event carries the
>   recipient (no auth lookup in notification). `GET /api/reservations/{id}` now includes tickets.
> - Realtime: `SeatsStatusChanged`/`ReservationStatusChanged` are published after commit; the STOMP
>   send is deferred to SPEC-0013. The failed-payment email is omitted in v1 (user-queue only).

## Goal

The customer confirms a `PENDING` reservation, which starts an async
payment; on payment success the reservation becomes `CONFIRMED`, seats turn
`SOLD`, tickets are issued and emailed.

## Scope

`booking` module: the confirm endpoint (`PENDING → AWAITING_PAYMENT`), the
listeners for `PaymentSucceeded`/`PaymentFailed`
(`AWAITING_PAYMENT → CONFIRMED | CANCELLED`), ticket issuance, realtime and
email triggers. Payment mechanics are 0015.

## Business Rules

- Only the reservation owner may confirm. Only `PENDING` reservations can
  be confirmed; expired (past `expiresAt`) cannot — even if the expiration
  job (0017) hasn't swept them yet (checked against injected `Clock`).
- Confirm transitions `PENDING → AWAITING_PAYMENT`, sets
  `payment_deadline_at = now + 10 min`
  (`app.booking.payment-deadline-minutes`), requests payment via the
  gateway port with the snapshotted `total_cents`, and returns **202** with
  the `paymentId`. Seats remain `HELD` while awaiting payment.
- Confirm is idempotent: re-confirming an `AWAITING_PAYMENT` reservation
  returns 202 with the same `paymentId` (no second charge).
- On `PaymentSucceeded` (after-commit listener, own transaction):
  `AWAITING_PAYMENT → CONFIRMED`; all reservation seats `HELD → SOLD`;
  one ticket per seat is issued; `ReservationConfirmed` event → email
  (0006) with tickets; realtime: seats `SOLD` on the public topic +
  `RESERVATION_STATUS_CHANGED: CONFIRMED` on the owner's queue.
- On `PaymentFailed`: `AWAITING_PAYMENT → CANCELLED`; seats `HELD → FREE`;
  realtime both channels; email optional (v1: user queue message only —
  the user is watching the screen at this moment).
- `SOLD` seats NEVER become `FREE` through this flow (only via cancellation
  0018).
- Late success webhook for a non-`AWAITING_PAYMENT` reservation → automatic
  refund request + WARN (`payment.late-success-refunded`), no state change.
- Ticket: `code` format `FKM-YYYY-NNNNNN` — `YYYY` = issuance year, `NNNNNN`
  = zero-padded value from a single global Postgres sequence
  (`ticket_code_seq`). No annual reset (avoids reset locking complexity;
  uniqueness by sequence; the year is informational). `seatLabel` like
  `A1`. Status `VALID | CANCELLED`.

## Input/Output Examples

```http
POST /api/reservations/{reservationId}/confirm
Authorization: Bearer <jwt>
→ 202 Accepted
{ "reservationId": "uuid", "status": "AWAITING_PAYMENT",
  "paymentId": "uuid", "paymentDeadlineAt": "2026-06-20T19:12:00Z" }
```

After webhook success, `GET /api/reservations/{id}`:

```json
{ "reservationId": "uuid", "status": "CONFIRMED",
  "tickets": [ { "ticketId": "uuid", "code": "FKM-2026-000001",
                 "seatLabel": "A1", "status": "VALID" } ] }
```

## API Contracts

- `POST /api/reservations/{id}/confirm` — owner; 202.
- Final state is observed via `GET /api/reservations/{id}` (0014) and the
  user realtime queue (0013).

## Events

Publishes: `ReservationConfirmed(reservationId, userId, tickets…)`,
`ReservationPaymentFailed(reservationId, userId)`. Consumes:
`PaymentSucceeded`, `PaymentFailed` (0015).

## Persistence Changes

- `tickets(id UUID PK, tenant_id, reservation_seat_id FK UNIQUE, code
   UNIQUE, status, issued_at)`.
- Sequence `ticket_code_seq`.
- Reuses `reservations.payment_deadline_at` (0014).

## Validation Rules

- Ownership; state machine transitions throw specific exceptions
  (`ReservationCannotBeConfirmedException` etc.).
- Amount passed to gateway MUST equal `reservations.total_cents`.

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 403 | `booking.not-owner` | Not the reservation owner. |
| 404 | `booking.reservation-not-found` | Unknown id. |
| 409 | `booking.already-confirmed` | Status CONFIRMED. |
| 410 | `booking.reservation-expired` | Past `expiresAt` (or EXPIRED). |
| 409 | `booking.reservation-cancelled` | Status CANCELLED. |

## Observability Requirements

- Metrics: `reservations_confirm_requested_total`,
  `reservations_confirmed_total`, `reservations_payment_failed_total`,
  `late_success_refunds_total`, time-to-confirm histogram (202 → webhook).
- Audit: confirmation, ticket issuance, late-success refunds.

## Tests Required

- Integration end-to-end with mock gateway: confirm → 202 → webhook →
  CONFIRMED + SOLD + tickets + email enqueued + both realtime messages.
- Integration: failed payment path releases seats to FREE.
- Integration: idempotent re-confirm returns same paymentId.
- Integration: expired-but-unswept reservation rejected with 410.
- Integration: late success → refund requested, state unchanged.
- Unit: ticket code generation format and uniqueness under concurrency.

## Acceptance Criteria

- Demo flow: reserve → confirm → ~3 s → tickets visible, email received,
  second browser sees seats SOLD live.

## Open Questions

- Payment deadline 10 min — confirm (config default).

## Out of Scope

- Real payment UI (mock requires no card form; the frontend shows a
  "processing" state — 0024). Refund (0018). Expiration (0017).
