# 0020 - Operator Ticket Lookup and Reprint

Status: Draft
Related ADRs: 0001

## Goal

Counter staff (OPERATOR) look up a customer's reservation/tickets and
produce a printable ticket view when the customer requests it. Per owner
decision, the operator only consults and prints — no selling, no
cancelling.

## Scope

`booking` module operator read endpoints + a print-friendly ticket
rendering (HTML in v1). Reprints are audited.

## Business Rules

- Access restricted to `OPERATOR | ADMIN`.
- Search by: ticket `code` (exact), reservation id (exact), or customer
  email (exact, case-insensitive) — returns matching reservations with
  status and screening info.
- Detail view: full reservation (0019 payload) regardless of owner.
- Reprint: `POST /api/operator/tickets/{ticketId}/reprint` registers an
  audited reprint record and returns the printable payload (the frontend
  renders a print-friendly page — 0026). Only `VALID` tickets of
  `CONFIRMED` reservations are reprintable; reprint does NOT change ticket
  state.
- Reprint count is visible to operators (fraud awareness — a ticket
  reprinted 5 times is a flag), but v1 imposes no hard limit.

## Input/Output Examples

```http
GET /api/operator/reservations?ticketCode=FKM-2026-000001
→ 200 [ { "reservationId": "uuid", "status": "CONFIRMED",
          "customerName": "Frank", "customerEmail": "f***@example.com",
          "movieTitle": "Alien", "startsAt": "...",
          "seatLabels": ["A5"] } ]
```

Email is partially masked in list results; full email visible in detail
(operator legitimately needs it to verify identity).

## API Contracts

- `GET /api/operator/reservations?ticketCode=|reservationId=|email=` —
  exactly one criterion required.
- `GET /api/operator/reservations/{id}` — detail.
- `POST /api/operator/tickets/{ticketId}/reprint`.

## Events

- `TicketReprinted(ticketId, operatorUserId, occurredAt)` — audit.

## Persistence Changes

- `ticket_reprints(id PK, ticket_id FK, operator_user_id FK, reprinted_at)`.
- Index `tickets(code)` (unique already), `users(email)` reused.

## Validation Rules

- Exactly one search criterion (400 otherwise); role check; ticket/
  reservation state guards for reprint.

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 400 | `operator.invalid-search` | Zero or multiple criteria. |
| 403 | `auth.forbidden` | Customer token. |
| 404 | `booking.ticket-not-found` | Unknown ticket. |
| 409 | `booking.ticket-not-reprintable` | Cancelled ticket / reservation. |

## Observability Requirements

- Audit every search (operator id + criterion type, not the raw email) and
  every reprint. Metric `ticket_reprints_total`.

## Tests Required

- Integration: search by each criterion; multiple criteria → 400.
- Integration: reprint valid ticket → audited; cancelled → 409; customer
  role → 403.

## Acceptance Criteria

- An operator finds a reservation by ticket code and produces a printable
  view in under three clicks (frontend, 0026).

## Open Questions

- Should reprint require a reason field? v1: no (friction at the counter);
  revisit if fraud appears.

## Out of Scope

- Counter sales, operator cancellation, QR-code validation at the room
  entrance (future ticket-validation spec), PDF generation (HTML print
  view in v1).
