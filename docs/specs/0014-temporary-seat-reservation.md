# 0014 - Temporary Seat Reservation

Status: Draft
Related ADRs: 0004, 0003, 0009

## Goal

An authenticated, email-verified customer holds one or more FREE seats for a
screening for 5 minutes, with the total price snapshotted, atomically and
safe under concurrency. Double booking MUST be impossible.

## Scope

`booking` module: `Reservation`, `ReservationSeat` entities, the critical
`FREE → HELD` transition, price snapshotting via the pricing module, and
realtime publication. Confirmation (0016), payment (0015), expiration
(0017) and cancellation (0018) build on this.

## Business Rules

- Caller MUST be `CUSTOMER` with `email_verified_at` set (0004). Operators/
  admins do not reserve in v1.
- Screening MUST be `SCHEDULED` and `startsAt` in the future. Reservations
  are rejected when `startsAt - now < 10 minutes` (no holding seats for a
  session about to start).
- 1–8 seats per reservation (`app.booking.max-seats-per-reservation`,
  default 8).
- Reservation is all-or-nothing: if ANY selected seat is not `FREE`, the
  whole request fails with the list of unavailable seats.
- A user MAY have at most ONE reservation in `PENDING | AWAITING_PAYMENT`
  per screening (enforced by partial unique index). Reservations on
  different screenings are independent.
- Each selected seat carries a `ticketType` (`FULL | HALF`); `HALF`
  requires `halfPriceCategory` + `documentReference` (0012).
- COMPANION seats MAY only be selected together with an ACCESSIBLE seat in
  the same reservation (companion exists to accompany).
- On success: seats transition `FREE → HELD`; the reservation is `PENDING`
  with `expiresAt = now + 5 min`
  (`app.booking.hold-minutes`, default 5); per-seat price is computed by
  pricing (0012) and **snapshotted** on `reservation_seats`; total stored
  on the reservation.
- Concurrency (ADR 0004): the service loads the target `ScreeningSeat`
  rows `FOR UPDATE` (pessimistic write lock) ordered by id (consistent
  ordering avoids deadlocks), verifies all are FREE, then transitions.
  Reservation rows carry `@Version`.
- After commit: publish `SeatsStatusChanged(HELD)` → realtime (0013).
- Backend is the source of truth; the frontend selection is advisory.

## Input/Output Examples

```http
POST /api/screenings/{screeningId}/reservations
Authorization: Bearer <jwt>
{
  "seats": [
    { "seatId": "uuid-1", "ticketType": "FULL" },
    { "seatId": "uuid-2", "ticketType": "HALF",
      "halfPriceCategory": "STUDENT", "documentReference": "UNE 123456" }
  ]
}
```

```http
201 Created
{
  "reservationId": "uuid", "screeningId": "uuid", "status": "PENDING",
  "expiresAt": "2026-06-20T19:05:00Z",
  "totalCents": 4500,
  "seats": [
    { "seatId": "uuid-1", "row": "A", "number": 5, "type": "STANDARD",
      "ticketType": "FULL", "priceCents": 3000 },
    { "seatId": "uuid-2", "row": "A", "number": 6, "type": "STANDARD",
      "ticketType": "HALF", "priceCents": 1500 }
  ]
}
```

## API Contracts

- `POST /api/screenings/{screeningId}/reservations` — authenticated
  customer. 201 on success.
- `GET /api/reservations/{id}` — owner only (or OPERATOR/ADMIN); current
  state incl. seats, prices, expiresAt.

## Events

- Internal `SeatsStatusChanged(screeningId, seats→HELD)` after commit.
- `ReservationCreated(reservationId, userId, screeningId, totalCents,
  occurredAt)` — audit/metrics.

## Persistence Changes

- `reservations(id UUID PK, tenant_id, user_id FK, screening_id FK, status,
   total_cents INT, expires_at timestamptz, payment_deadline_at NULL,
   version BIGINT, created_at, updated_at)`;
   status ∈ `PENDING | AWAITING_PAYMENT | CONFIRMED | CANCELLED | EXPIRED`.
- Partial unique index:
  `UNIQUE(user_id, screening_id) WHERE status IN ('PENDING','AWAITING_PAYMENT')`.
- `reservation_seats(id PK, reservation_id FK, screening_seat_id FK,
   ticket_type, half_price_category NULL, document_reference NULL,
   price_cents INT)`; `UNIQUE(reservation_id, screening_seat_id)`.
- Index `reservations(status, expires_at)` for the expiration job.

## Validation Rules

- Boundary: payload shape, 1–8 seats, half-price fields conditional.
- Application: verified email, screening window, ownership of state checks.
- Domain: status transition methods on `Reservation` and `ScreeningSeat`
  throw specific business exceptions on invalid transitions.
- Persistence: partial unique index, FKs, CHECKs.

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 401/403 | `auth.*` | Unauthenticated / not customer. |
| 403 | `user.email-not-verified` | Verified email required. |
| 404 | `screening.not-found` | Unknown screening. |
| 409 | `booking.seats-unavailable` | ≥1 seat not FREE; `fields` lists them. |
| 409 | `booking.active-reservation-exists` | User already holds this screening. |
| 409 | `booking.companion-requires-accessible` | COMPANION without ACCESSIBLE. |
| 422 | `booking.screening-too-soon` | < 10 min to start. |
| 400 | `booking.invalid-request` | Shape/limits violations. |

## Observability Requirements

- Metrics: `reservations_created_total`, `reservation_create_latency`
  (histogram), `pessimistic_lock_wait_seconds` (ADR 0004 revision
  triggers), `reservations_rejected_total{reason}`.
- Log with `userId`, `screeningId`, seat count, outcome. Audit:
  `ReservationCreated`.

## Tests Required

- Integration: happy path 1 seat and N seats with mixed FULL/HALF; snapshot
  prices match 0012 quotes.
- Integration: all-or-nothing on one HELD/SOLD seat.
- Integration: **concurrency** — two parallel transactions targeting the
  same seat: exactly one succeeds (Testcontainers + executor).
- Integration: partial unique index blocks second active reservation.
- Integration: COMPANION rule; email-not-verified rejection; too-soon rule.
- Integration: realtime message after commit, not on rollback.

## Acceptance Criteria

- Under a 50-thread hammer test on one seat, exactly one reservation wins
  and the seat map shows HELD once.

## Open Questions

- Max 8 seats and the one-active-reservation-per-screening rule — owner
  confirm (both configurable).

## Out of Scope

- Confirmation/payment (0015/0016), expiration (0017), cancellation (0018),
  frontend (0024).
