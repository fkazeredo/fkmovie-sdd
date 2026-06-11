# 0008 - Movies CRUD (Admin)

Status: Draft
Related ADRs: 0001

## Goal

Admins manage the movie catalog manually. Movies feed screenings (0009) and
the public list (0010).

## Scope

`screening` module, `Movie` entity, admin REST API. Manual entry only (no
TheMovieDB integration in v1, per owner decision — design does not block it).

## Business Rules

- A `Movie` has: `title` (required, 1–200 chars), `durationMinutes`
  (required, 1–600 — used by 0009 for overlap validation), `synopsis`
  (optional, ≤2000), `posterUrl` (optional, valid URL), `ageRating`
  (required, Brazilian Classificação Indicativa: `L | A10 | A12 | A14 |
  A16 | A18`), `status` (`ACTIVE | ARCHIVED`).
- Title is NOT unique (re-releases exist); duplicates produce a warning in
  the admin UI (0026), not an error.
- A movie with existing screenings MUST NOT be deleted — it is archived.
  A movie without screenings MAY be hard-deleted.
- Archived movies don't appear in screening creation (0009) nor in public
  list (0010); existing screenings keep working.
- Only `ADMIN` may access this API.

## Input/Output Examples

```http
POST /api/admin/movies
{ "title": "Alien", "durationMinutes": 117, "ageRating": "A14",
  "synopsis": "...", "posterUrl": "https://..." }
→ 201 { "id": "uuid", "title": "Alien", ..., "status": "ACTIVE" }
```

## API Contracts

- `POST /api/admin/movies` — create.
- `GET /api/admin/movies?status=&search=&page=&size=` — paginated, search by
  title (case-insensitive contains).
- `GET /api/admin/movies/{id}`.
- `PUT /api/admin/movies/{id}` — full update (no sold-ticket restriction;
  movie data is descriptive).
- `POST /api/admin/movies/{id}/archive` / `POST .../unarchive`.
- `DELETE /api/admin/movies/{id}` — only when no screenings reference it.

## Events

- `MovieArchived(movieId, occurredAt)` — internal; 0010 cache (if any)
  invalidation hook.

## Persistence Changes

- `movies(id UUID PK, tenant_id, title, duration_minutes, synopsis,
   poster_url, age_rating, status, created_at, updated_at)`.
- Index: `(tenant_id, status)`, trigram or `LOWER(title)` index for search.

## Validation Rules

- Bean Validation at the boundary; CHECK constraints for enums; URL format
  for poster.

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 401/403 | `auth.unauthorized` / `auth.forbidden` | Not admin. |
| 404 | `movie.not-found` | Unknown id. |
| 409 | `movie.has-screenings` | DELETE with screenings present. |
| 400 | `movie.invalid-request` | Validation failure (with `fields`). |

## Observability Requirements

- Audit log every admin mutation: `actingUserId`, `movieId`, `action`.
- Metric: `admin_movies_mutations_total{action=...}`.

## Tests Required

- Integration: CRUD happy paths; delete blocked with screenings; archive
  hides from active listing.
- Integration: non-admin 403.

## Acceptance Criteria

- Admin can create, edit, archive and (when unused) delete movies.

## Open Questions

None.

## Out of Scope

- TheMovieDB integration (future; `posterUrl` and external-id column can be
  added then). Public listing (0010). Screening management (0009).
