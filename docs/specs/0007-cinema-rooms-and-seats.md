# 0007 - Cinema Rooms and Seats with Types

Status: Draft
Related ADRs: 0001, 0003

## Goal

Model the physical cinema structure: 4 rooms with seats, each seat having a
type that drives pricing (0012) and rendering (0023).

## Scope

`cinema` module: `CinemaRoom` and `Seat` entities, Flyway migrations, seed.
No REST endpoints (rooms are seed-managed in v1), no availability — a
physical `Seat` has NO availability status (availability belongs to
`ScreeningSeat`, spec 0011).

## Business Context

The cinema has 4 fixed rooms. Seat types follow standard Brazilian cinema:
STANDARD, VIP (premium recliner), ACCESSIBLE (wheelchair space, ABNT NBR
9050) and COMPANION (seat adjacent to an accessible space).

## Business Rules

- Exactly 4 rooms in the initial seed; room name is unique.
- A seat belongs to one room; has `row` (letter), `number` (starts at 1) and
  `type` (`STANDARD | VIP | ACCESSIBLE | COMPANION`).
- A room MUST NOT have two seats with the same row+number.
- A physical `Seat` MUST NOT have availability status.
- Seed layout:
  - Room 1: 8 rows (A–H) x 10 seats — 80 seats.
  - Room 2: 8 rows (A–H) x 10 seats — 80 seats.
  - Room 3: 10 rows (A–J) x 12 seats — 120 seats.
  - Room 4: 6 rows (A–F) x 8 seats — 48 seats.
- Seed seat types (defaults; rooms are seed-managed in v1):
  - Row A seats 1–2: `ACCESSIBLE`; row A seats 3–4: `COMPANION`.
  - Last row of each room: `VIP`.
  - All remaining: `STANDARD`.

## Persistence Changes

- `cinema_rooms(id UUID PK, tenant_id, name UNIQUE, created_at, updated_at)`.
- `seats(id UUID PK, room_id FK, row VARCHAR(2), number INT, type,
   created_at)` with `UNIQUE(room_id, row, number)`.
- Flyway migration creates tables; a separate repeatable-safe seed migration
  inserts rooms and seats (idempotent: skips if rooms exist).

## Validation Rules

- DB constraints are the source of integrity (unique, FK, NOT NULL,
  CHECK on `type`).
- Domain: `Seat` constructor validates row format (A–Z) and number ≥ 1.

## Error Behavior

Not applicable (no API).

## Observability Requirements

- Log seed execution outcome at startup migration.

## Tests Required

- Integration: seed creates 4 rooms with 80/80/120/48 seats.
- Integration: duplicate row+number in the same room rejected by constraint.
- Unit: `Seat` has no availability field/methods (architectural assertion).
- Integration: seat type distribution matches the seed rules.

## Acceptance Criteria

- `mvn verify` green with migrations applied on Testcontainers.
- Seed idempotent: running migrations twice does not duplicate.

## Open Questions

- Room/seat admin CRUD (rename room, change seat types) is v1-out. Confirm
  acceptable: changing a seat type requires a migration in v1.

## Out of Scope

- REST endpoints, movies, screenings, availability, reservations, realtime,
  frontend, room CRUD.
