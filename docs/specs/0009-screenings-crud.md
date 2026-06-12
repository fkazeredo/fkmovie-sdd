# 0009 - Screenings CRUD (Admin)

Status: Implemented
Related ADRs: 0001, 0009

> Implementation notes (backend):
> - Cleaning buffer resolved (Open Question): 30 min, configurable via
>   `app.screening.buffer-minutes`. `endsAt = startsAt + movie duration + buffer`.
> - Room overlap is guaranteed by a Postgres `btree_gist` `EXCLUDE` constraint over
>   `tstzrange(starts_at, ends_at)` where `status = 'SCHEDULED'`; the service pre-checks for
>   the friendly 409 and also maps the constraint violation to `screening.room-overlap`.
> - First synchronous cross-module read: the `cinema` module now exposes a public
>   `CinemaCatalog`/`SeatView` facade (harmonizes the read seam deferred in 0007). The
>   booking materializer reads room seats through it, never the `Seat` entity.
> - GET `/{id}` returns screening fields only. The seat inventory summary the contract
>   mentions is **deferred to 0011**: having the screening admin GET read booking would make
>   `screening → booking` cyclic (booking already depends on screening via `ScreeningCreated`)
>   and break Modulith `verify()`. The seat map/inventory is booking's public endpoint (0011).
> - Seat materialization runs in the booking module via an AFTER_COMMIT consumer of
>   `ScreeningCreated` (REQUIRES_NEW, idempotent; `UNIQUE(screening_id, seat_id)`). The
>   `screening_seats` table (owned by 0011) is created here because the consumer needs it.
> - Harmonized the 0008 movie deletion guard: `movie.has-screenings` 409 is now live.
> - Edit/cancel guards against reservations/sold tickets are **inert** (wired but no-op)
>   until reservations exist (0014); `screening.has-reservations`/`screening.has-sold-tickets`
>   and the `ScreeningModificationGuard` are the deferred seam.
> - `ScreeningCancelled` is published but the booking module takes no seat action in v1 (the
>   CANCELLED status blocks new reservations; no sold tickets by rule).
> - Movie that is missing OR not ACTIVE → `screening.movie-not-found` (404).

## Goal

Admins schedule screenings (movie + room + start time + base price).
Creating a screening materializes its `ScreeningSeat` inventory in the
booking module.

## Scope

`screening` module: `Screening` entity, admin REST API, overlap validation,
`ScreeningCreated` event. The `booking` module consumes the event to create
`screening_seats` rows (cross-module via event, per ADR 0001).

## Business Rules

- A `Screening` has: `movieId` (required, ACTIVE movie), `roomId`
  (required), `startsAt` (required, future, UTC instant),
  `basePriceCents` (required, > 0, BRL), `status` (`SCHEDULED | CANCELLED`).
- End time is derived: `startsAt + movie.durationMinutes + 30 min` cleaning
  buffer.
- Two `SCHEDULED` screenings in the same room MUST NOT overlap (considering
  the buffer).
- `startsAt` MUST be at least 1 hour in the future at creation time.
- Editing `startsAt`, `roomId` or `movieId` is allowed only while the
  screening has NO reservation in `PENDING | AWAITING_PAYMENT | CONFIRMED`
  for any of its seats. `basePriceCents` follows the same rule (prices are
  snapshotted at reservation, but changing the public price after sales is
  commercially confusing — blocked in v1).
- Deleting is replaced by status `CANCELLED`; only allowed with no sold
  tickets. Cancelling a screening WITH sold tickets (mass refund) is out of
  scope v1.
- On creation, publish `ScreeningCreated(screeningId, roomId)`; booking
  module creates one `FREE` `ScreeningSeat` per physical seat of the room
  (after commit, idempotent).
- Only `ADMIN` may access this API.

## Input/Output Examples

```http
POST /api/admin/screenings
{ "movieId": "uuid", "roomId": "uuid",
  "startsAt": "2026-06-20T20:00:00Z", "basePriceCents": 3000 }
→ 201 { "id": "uuid", ..., "endsAt": "2026-06-20T22:27:00Z",
        "status": "SCHEDULED" }
```

## API Contracts

- `POST /api/admin/screenings`.
- `GET /api/admin/screenings?roomId=&movieId=&from=&to=&page=&size=`.
- `GET /api/admin/screenings/{id}` — includes seat inventory summary
  (free/held/sold counts) read from booking's public API.
- `PUT /api/admin/screenings/{id}` — subject to the no-reservations rule.
- `POST /api/admin/screenings/{id}/cancel` — only without sold tickets.

## Events

- `ScreeningCreated(screeningId, roomId, occurredAt)` — consumed by booking
  to materialize `screening_seats`. Idempotent consumer (unique constraint
  makes replay safe).
- `ScreeningCancelled(screeningId, occurredAt)` — booking releases nothing
  (no sold tickets by rule) but marks remaining seats unavailable for new
  reservations.

## Persistence Changes

- `screenings(id UUID PK, tenant_id, movie_id FK, room_id FK, starts_at
   timestamptz, ends_at timestamptz, base_price_cents INT, status,
   created_at, updated_at)`.
- Overlap enforcement: Postgres exclusion constraint
  `EXCLUDE USING gist (room_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
   WHERE (status = 'SCHEDULED')` (requires `btree_gist`). Application-level
  check provides the friendly error; the constraint is the guarantee.

## Validation Rules

- Boundary: ids, instant format, price > 0.
- Application: movie ACTIVE, room exists, 1h-future rule, overlap pre-check.
- Persistence: exclusion constraint, FKs.

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 404 | `screening.movie-not-found` / `screening.room-not-found` | Bad refs. |
| 409 | `screening.room-overlap` | Overlaps another scheduled screening. |
| 409 | `screening.has-reservations` | Edit blocked by existing reservations. |
| 409 | `screening.has-sold-tickets` | Cancel blocked. |
| 400 | `screening.starts-too-soon` | < 1h in the future. |

## Observability Requirements

- Audit log admin mutations; metric
  `admin_screenings_mutations_total{action=...}`.
- Booking consumer logs materialization: `screeningId`, seats created.

## Tests Required

- Integration: create → booking materializes correct seat count per room.
- Integration: overlap rejected (app check and DB constraint).
- Integration: edit blocked when reservations exist.
- Integration: event consumer idempotent (replay creates nothing).

## Acceptance Criteria

- Admin schedules a screening; seat map (0011) immediately shows all seats
  FREE.
- Overlapping screening in the same room is impossible.

## Open Questions

- Cleaning buffer 30 min — confirm with owner (configurable via property
  `app.screening.buffer-minutes`, default 30).

## Out of Scope

- Mass cancellation with refunds. Public listing (0010). Recurring
  scheduling templates.
