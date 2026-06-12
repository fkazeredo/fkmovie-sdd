# 0011 - Seat Map Read Model

Status: Implemented
Related ADRs: 0001, 0004

> Implementation notes (backend):
> - `GET /api/screenings/{screeningId}/seats` is public (permitAll); the table
>   `screening_seats` and the inventory were created/materialized in 0009.
> - `fullPriceCents` is a deferred pricing seam (`SeatPricing`, see
>   `architecture/simulation-and-mocking.md`): it returns the screening's flat base price for
>   every seat type until SPEC-0012 ships the real formula (per-type surcharge × weekday
>   multiplier). The map is informational; reservations (0014) snapshot the real price.
> - The read joins three modules via public facades: screening `ScreeningCatalog`/`ScreeningView`
>   (new), cinema `CinemaCatalog`/`SeatView`/`RoomView`, and booking's own `screening_seats`.
>   `booking → screening` stays one-directional (no module cycle).
> - Errors: 404 reuses `screening.not-found`; 410 `screening.cancelled` is introduced here.
> - The assembled read model (`SeatMapResponse`/`SeatMapSeat`) lives in the booking root (built
>   by the service), not the `api` package, to honor the core-must-not-depend-on-api rule.
> - Realtime patching of this read model is SPEC-0013.

## Goal

Anyone (auth not required for viewing) sees the seat map of a screening with
per-seat status, type and full price. This is the read model the realtime
updates (0013) patch.

## Scope

`booking` module read endpoint. `ScreeningSeat` entity (created by the 0009
event consumer). No mutation here.

## Business Rules

- Availability belongs to `ScreeningSeat` (`FREE | HELD | SOLD`); the
  physical `Seat` has no status.
- Every screening has exactly one `ScreeningSeat` per physical seat of its
  room; all start `FREE`.
- Response includes per seat: `seatId`, `row`, `number`, `type`, `status`,
  `fullPriceCents` (computed by pricing 0012 for that screening; half price
  is derived client-side as the law's 50% but ALWAYS recomputed server-side
  at reservation).
- Unknown screening → 404. Cancelled screening → 410.
- Response MUST NOT expose entities.

## Input/Output Examples

```http
GET /api/screenings/{screeningId}/seats
→ 200
{
  "screeningId": "uuid",
  "roomName": "Room 1",
  "startsAt": "2026-06-20T20:00:00Z",
  "seats": [
    { "seatId": "uuid", "row": "A", "number": 1, "type": "ACCESSIBLE",
      "status": "FREE", "fullPriceCents": 3000 }
  ]
}
```

## API Contracts

- `GET /api/screenings/{screeningId}/seats` — public.

## Events

Consumes `ScreeningCreated` (0009) to materialize rows — implemented in 0009;
listed here because the table is owned by this spec.

## Persistence Changes

- `screening_seats(id UUID PK, tenant_id, screening_id FK, seat_id FK,
   status, version BIGINT, updated_at)` with
   `UNIQUE(screening_id, seat_id)` and CHECK on status.
- Index `(screening_id, status)` for the seat map and inventory counts.
- `version` supports the locking strategy of ADR 0004.

## Validation Rules

- Path UUID format; existence checks in application layer.

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 404 | `screening.not-found` | Unknown id. |
| 410 | `screening.cancelled` | Screening was cancelled. |

## Observability Requirements

- Latency histogram (hot endpoint); metric
  `seat_map_requests_total{outcome}`.

## Tests Required

- Integration: map returns all room seats, all FREE on a fresh screening.
- Integration: duplicate `ScreeningSeat` rejected by constraint.
- Integration: 404 unknown, 410 cancelled.
- Contract: physical `Seat` has no status field anywhere in the response.

## Acceptance Criteria

- Fresh screening shows the full room FREE with correct types and prices.

## Open Questions

None.

## Out of Scope

- Reservation (0014), realtime (0013), expiration (0017), frontend (0023).
