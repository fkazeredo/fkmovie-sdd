# 0018 - Customer Cancellation and Refund

Status: Implemented
Related ADRs: 0006

> Implementation notes (backend):
> - `POST /api/reservations/{id}/cancel` (200) → `ReservationCancellationService`. PENDING/
>   AWAITING_PAYMENT release HELD seats (no refund); CONFIRMED, while `startsAt - now >= 2h`
>   (`app.booking.cancellation-window-hours`, default 2, `>=` boundary open), cancels tickets
>   (VALID→CANCELLED), returns SOLD seats to inventory (`ScreeningSeat.releaseFromSold` — the audited
>   exception to 0016's "SOLD never becomes FREE") and requests a full refund via the gateway port.
>   The refund settles asynchronously and never gates the cancellation.
> - `CancellationPublisher` emits realtime FREE/CANCELLED (the 0013 publishers send the STOMP), the
>   audit `ReservationCancelled(reason=CUSTOMER, refundRequested)` and a contact-carrying
>   `ReservationCancellationConfirmed` consumed by notification.
> - Decision: **one** cancellation email at cancel time (mentions the refund when applicable);
>   `RefundSucceeded` needs no booking action (the `payments` table is the ledger); `RefundFailed`
>   raises an ERROR audit + `refunds_failed_total` (`RefundEventListener`).
> - Decision: reused existing error codes — `auth.forbidden` (403), `reservation.not-found` (404),
>   `booking.reservation-cancelled` (409, already-cancelled), `booking.reservation-expired` (410) —
>   plus the one new `booking.cancellation-window-closed` (409). HTTP statuses match the table below.
> - Metrics: `reservations_cancelled_total{previousStatus}`, `refunds_requested_total`,
>   `refunds_failed_total`. No schema change. Open Question (refund SLA copy) deferred to 0025.

## Goal

Customers cancel their own reservations. Confirmed reservations are
refundable until 2 hours before the session start; the refund goes through
the gateway port (mock in v1).

## Scope

`booking` module cancellation use case + `RefundSucceeded/Failed` listeners.
Operator-initiated cancellation is out of scope (future spec).

## Business Rules

- Only the owner cancels. Cancellable states:
  - `PENDING` → `CANCELLED` immediately; seats `HELD → FREE`. No payment
    involved.
  - `AWAITING_PAYMENT` → `CANCELLED`; seats released. If a success webhook
    arrives later, 0016's late-success rule auto-refunds.
  - `CONFIRMED` → allowed only while
    `screening.startsAt - now >= 2 hours` (cancellation window, Cinemark-
    style policy chosen by owner). Transitions to `CANCELLED`; tickets
    `VALID → CANCELLED`; seats `SOLD → FREE` (back to inventory); a refund
    request for `total_cents` is sent via the gateway port.
  - `EXPIRED`/`CANCELLED` → 409/410 (nothing to cancel).
- The `SOLD → FREE` release on confirmed-cancellation is the ONLY path that
  frees sold seats (0016's "SOLD never becomes FREE" applies to the payment
  flow; this spec is the deliberate exception, audited).
- Refund is full (100%) in v1. The refund result does NOT gate the
  cancellation: the reservation is `CANCELLED` immediately; refund settles
  async. `RefundFailed` raises an ERROR audit + metric for manual ops
  follow-up (mock rarely fails; real gateways do).
- Inside the 2-hour window: self-service cancellation is rejected; the
  ticket remains valid. (CDC 7-day withdrawal right is honored within the
  same operational window — documented product decision.)
- After commit: realtime seats `FREE` + owner-queue status message; email
  confirmation of cancellation/refund (0006).

## Input/Output Examples

```http
POST /api/reservations/{reservationId}/cancel
Authorization: Bearer <jwt>
→ 200
{ "reservationId": "uuid", "status": "CANCELLED",
  "refund": { "requested": true, "amountCents": 4500 } }
```

(`refund.requested = false` for PENDING cancellations.)

## API Contracts

- `POST /api/reservations/{id}/cancel` — owner only.

## Events

Publishes `ReservationCancelled(reservationId, userId, reason=CUSTOMER,
refundRequested)`; consumes `RefundSucceeded/RefundFailed` (0015) to update
the payment ledger view and notify by email on success.

## Persistence Changes

None new — `payments` (kind REFUND) from 0015; `tickets.status` from 0016.

## Validation Rules

- Ownership; window check against injected `Clock`; state-machine guards
  with specific exceptions.

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 403 | `booking.not-owner` | Not the owner. |
| 404 | `booking.reservation-not-found` | Unknown id. |
| 409 | `booking.cancellation-window-closed` | < 2h to start (CONFIRMED). |
| 409 | `booking.already-cancelled` | Already CANCELLED. |
| 410 | `booking.reservation-expired` | EXPIRED. |

## Observability Requirements

- Metrics: `reservations_cancelled_total{previousStatus}`,
  `refunds_requested_total`, `refunds_failed_total` (alerting candidate).
- Audit: cancellation with previous status, refund linkage.

## Tests Required

- Integration: cancel PENDING (no refund), AWAITING_PAYMENT, CONFIRMED
  outside window (refund requested, seats FREE, tickets CANCELLED).
- Integration: CONFIRMED inside window → 409, nothing changes.
- Integration: window boundary exactly at 2h (>= passes) with test Clock.
- Integration: realtime + email triggers after commit.

## Acceptance Criteria

- A confirmed reservation cancelled 3h before start frees the seats live
  and produces a mock refund within seconds.

## Open Questions

- Refund SLA messaging to the user (mock is instant; real gateways take
  days) — copy decision for 0025, flagged now.

## Out of Scope

- Operator/admin cancellation and mass refunds. Partial refunds. Fees.
