# 0010 - Public Screenings List

Status: Draft
Related ADRs: 0001

## Goal

Anonymous users browse upcoming screenings to choose a session before
authenticating.

## Scope

Public read endpoint in the `screening` module. No authentication required.

## Business Rules

- Only `SCHEDULED` screenings of `ACTIVE` movies with `startsAt` in the
  future are listed.
- Default sort: `startsAt` ascending. Filters: `date` (calendar day in the
  cinema's timezone America/Sao_Paulo), `movieId`.
- Response MUST NOT expose entities; DTO only.
- Pagination: page/size, max size 100, default 20.
- Response includes the "from" price (cheapest possible full price for that
  screening, computed by pricing 0012) so the UI can show "a partir de
  R$ X".

## Input/Output Examples

```http
GET /api/screenings?date=2026-06-20&page=0&size=20
→ 200
{
  "content": [
    { "id": "uuid", "movieTitle": "Alien", "ageRating": "A14",
      "posterUrl": "https://...", "roomName": "Room 1",
      "startsAt": "2026-06-20T20:00:00Z", "durationMinutes": 117,
      "fromPriceCents": 2100 }
  ],
  "page": 0, "size": 20, "totalElements": 42, "totalPages": 3
}
```

## API Contracts

- `GET /api/screenings` — public, paginated, filterable. Shape above is the
  project-standard pagination envelope (`shared/pagination`).

## Events

Not applicable.

## Persistence Changes

None (reads `screenings` + `movies` + pricing config). Read query joins are
acceptable per `architecture/persistence.md` (reads are flexible).

## Validation Rules

- `date` is ISO `yyyy-MM-dd`; invalid → 400. `size` clamped to 100.

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 400 | `screening.invalid-filter` | Bad date/page/size. |

Empty result is `200` with empty `content` — never 404.

## Observability Requirements

- Metric: `public_screenings_list_total`; latency histogram (this is the
  highest-traffic read with seat map).

## Tests Required

- Integration: lists only future, scheduled, active-movie screenings.
- Integration: date filter respects America/Sao_Paulo day boundary.
- Integration: `fromPriceCents` equals cheapest full price from 0012 rules.
- Integration: response does not serialize entities (DTO contract test).

## Acceptance Criteria

- Anonymous request returns the upcoming sessions with correct pricing
  preview and pagination.

## Open Questions

- Cinema display timezone fixed at America/Sao_Paulo (single cinema). On
  multi-tenant future, timezone becomes tenant config. Confirmed default.

## Out of Scope

- Seat map (0011). Search by movie title (admin has it; public v1 filters
  only by date/movie). Caching (add only if metrics demand).
