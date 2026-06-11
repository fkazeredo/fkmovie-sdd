# 0022 - Angular Screenings List

Status: Draft
Related ADRs: 0008

## Goal

Public landing page: browse upcoming screenings (0010) and navigate to a
seat map. Works without authentication.

## Scope

`features/screenings`: list page, date/movie filtering, pagination.

## UI Behavior

- Route: `/screenings` (app default route redirects here).
- States: loading (skeleton cards), error (retry button), empty ("no
  sessions for this day").
- Card per screening: poster (fallback placeholder), movie title, age
  rating badge (BR Classificação colors), room name, start time formatted
  in America/Sao_Paulo with the active locale, duration, "a partir de
  R$ X" from `fromPriceCents`.
- Filters: date picker (PrimeNG Calendar, default today, locale-aware) and
  movie dropdown (distinct movies from current results — no extra
  endpoint in v1). Filters update the URL query params (shareable links).
- Pagination via PrimeNG Paginator bound to the 0010 envelope.
- Clicking a card navigates to `/screenings/:screeningId/seats`.
- Currency formatted as BRL with the active locale; dates via a shared
  `core` formatting service (single source for timezone handling).

## Architecture

`data-access/screenings-api.service.ts` (typed DTOs from 0010),
`pages/screenings-list.page.ts` (signals for filters/state; `httpResource`
or service+signal pattern — follow what 0002 established), `ui/`
(screening-card, filters-bar). No NgRx.

## Tests Required

- Page: loading/error/empty/success states; filter changes refetch and
  update URL; pagination wiring.
- Card: price/date/age-rating rendering per locale.
- Navigation to seat map with the right id.

## Acceptance Criteria

- Anonymous user filters by date and reaches a seat map in two clicks; the
  URL is shareable and restores filters.

## Open Questions

None.

## Out of Scope

- Seat map (0023), search by title, movie detail page, SSR/SEO.
