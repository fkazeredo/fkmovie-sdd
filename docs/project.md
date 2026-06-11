# fkmovies — Project Overview

Online ticketing for a single-location cinema with 4 rooms: customers
browse screenings, pick seats on a live seat map, hold them for 5 minutes,
pay (mock gateway in v1) and receive tickets by email. Staff have a
back-office: admins manage movies, screenings, pricing and users; counter
operators look up reservations and reprint tickets.

## Audience and roles

- **CUSTOMER** — self-registers (verified email required to reserve),
  reserves, pays, cancels (until 2h before the session, full refund),
  views history.
- **OPERATOR** — searches reservations/tickets, reprints. No selling, no
  cancelling (owner decision).
- **ADMIN** — invites staff, manages catalog (movies, screenings), pricing
  modifiers, users.

## Core invariants

- Double booking is impossible (ADR 0004: pessimistic lock on `FREE→HELD`
  + DB constraints + optimistic versioning).
- Seat availability lives on `ScreeningSeat` (`FREE | HELD | SOLD`), never
  on the physical `Seat`.
- Reservation state machine:
  `PENDING → AWAITING_PAYMENT → CONFIRMED | CANCELLED | EXPIRED`.
- Realtime messages are sent only after transaction commit (ADR 0009).
- Prices are snapshotted at reservation time; pricing config changes never
  affect existing reservations.
- Backend is the source of truth; the frontend never trusts local state.

## Stack

Java 21 / Spring Boot 4 modular monolith (Spring Modulith, ADR 0001),
Postgres + Flyway, STOMP WebSocket, JWT auth (ADR 0005). Angular 22 +
PrimeNG + Tailwind + ngx-translate (ADR 0008). Single Docker instance per
environment (ADR 0002). Single-tenant with multi-tenant extension points
(ADR 0003). PT-BR default, EN supported.

## Glossary

- **Screening** — a session of a movie in a room at a time.
- **ScreeningSeat** — the availability of one physical seat for one
  screening.
- **Hold** — 5-minute `HELD` state created by a pending reservation.
- **Meia-entrada** — Brazilian half-price right (Lei 12.933/2013), 50% of
  the full price, declaration-based with document reference stored.
- **Ticket** — issued per seat on confirmation; code `FKM-YYYY-NNNNNN`.

## Where things are decided

- `docs/specs/` — feature behavior (source of truth per feature).
- `docs/adr/` — architectural decisions and their reasons.
- `architecture/` — cross-cutting engineering rules (loaded on demand via
  `CLAUDE.md` routing map).
