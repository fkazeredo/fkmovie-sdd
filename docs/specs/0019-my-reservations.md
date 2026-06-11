# 0019 - My Reservations

Status: Draft
Related ADRs: 0001

## Goal

Customers list and inspect their own reservations and tickets.

## Scope

`booking` module read endpoints scoped to the authenticated user.

## Business Rules

- Returns only the caller's reservations (owner scoping is mandatory, not a
  filter parameter).
- List is paginated, newest first, filterable by `status` and by
  `upcoming=true` (screening in the future).
- Detail includes: screening info (movie title, room, startsAt), status,
  expiresAt/paymentDeadlineAt when applicable, seats with labels and
  snapshotted prices, total, tickets (code, seatLabel, status) when
  confirmed, refund summary when cancelled-with-refund.
- The detail endpoint is the same `GET /api/reservations/{id}` from 0014 —
  this spec extends its payload with tickets/refund and locks down owner
  scoping (OPERATOR/ADMIN also allowed for support, used by 0020).

## Input/Output Examples

```http
GET /api/me/reservations?status=CONFIRMED&upcoming=true&page=0&size=10
→ 200 (standard pagination envelope)
{ "content": [ { "reservationId": "uuid", "status": "CONFIRMED",
    "movieTitle": "Alien", "roomName": "Room 1",
    "startsAt": "2026-06-20T20:00:00Z", "totalCents": 4500,
    "seatLabels": ["A5", "A6"] } ], ... }
```

## API Contracts

- `GET /api/me/reservations` — authenticated customer.
- `GET /api/reservations/{id}` — owner or OPERATOR/ADMIN (0014 + extended
  payload).

## Events

None.

## Persistence Changes

None — read model over existing tables; index
`reservations(user_id, created_at DESC)`.

## Validation Rules

- Status filter from the closed enum; pagination clamped (max 50).

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 403 | `booking.not-owner` | Detail of someone else's reservation (customer). |
| 404 | `booking.reservation-not-found` | Unknown id. |

## Observability Requirements

- Latency metric on the list endpoint.

## Tests Required

- Integration: list returns only own reservations; filters work.
- Integration: customer fetching another user's detail → 403; operator →
  200.

## Acceptance Criteria

- A customer sees their history with correct statuses and tickets.

## Open Questions

None.

## Out of Scope

- Frontend (0025). Invoice/receipt documents. Export.
