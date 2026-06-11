# 0023 - Angular Seat Map Realtime

Status: Draft
Related ADRs: 0008, 0009

## Goal

The seat selection experience: render the room (0011), keep it live via
STOMP (0013), let the user compose a selection with ticket types, and hand
off to the reservation flow (0024).

## Scope

`features/seat-map` + `core/realtime` (STOMP client wrapper). The
reservation POST itself is 0024.

## UI Behavior

- Route: `/screenings/:screeningId/seats`. Viewing is public; attempting
  to proceed without login redirects to `/login?returnUrl=...`.
- States: loading, error (retry), cancelled screening (410 → friendly
  message + back link).
- Render seats grouped by row (A front), Tailwind CSS grid; screen
  indicator at top. Legend: FREE / HELD / SOLD / selected + seat types
  (STANDARD, VIP, ACCESSIBLE ♿, COMPANION) with price differences.
- Selecting: only FREE seats; max 8 (0014 limit mirrored client-side, with
  i18n message); COMPANION selectable only with an ACCESSIBLE seat in the
  selection (0014 rule mirrored — backend remains authority).
- Per selected seat: ticket type chooser (FULL / HALF). Choosing HALF opens
  category select + document reference input (0012/0014 contract). Summary
  panel: seats, per-seat price (full from 0011; half computed as displayed
  hint "50%" but flagged as confirmed at reservation), running total,
  CTA "Reservar" → 0024.
- Realtime: on init, connect (authenticated users) and subscribe to
  `/topic/screenings/{id}/seats`; merge `SEAT_STATUS_CHANGED` into the map
  signal. If an update marks a selected seat HELD/SOLD, remove it from the
  selection and toast-notify. Reconnect with backoff on drop; on
  reconnect, refetch the REST map (resync after gap). Anonymous users see
  the static map (0013 decision) — a subtle "log in for live updates" hint.
- Backend is the source of truth; the UI never assumes its selection
  succeeded.

## Architecture

`core/realtime/stomp.service.ts`: connection lifecycle, JWT on CONNECT,
typed subscribe helpers, reconnect/backoff, RxJS stream → consumed by
features. `features/seat-map`: `data-access/seat-map-api.service.ts`,
`pages/seat-map.page.ts` (signals: seats, selection, derived total),
`ui/` (seat, row, legend, summary-panel). `@for` with `track seat.seatId`.

## Tests Required

- Merge logic: realtime patch updates statuses; selected seat stolen →
  removed + notified.
- Selection rules: max 8, COMPANION/ACCESSIBLE pairing, HELD/SOLD not
  selectable.
- Reconnect: resync fetch after reconnect (service test with mocked stream).
- Page states incl. 410.

## Acceptance Criteria

- Two browsers: a hold in one appears in the other within 1 s; stealing a
  selected seat removes it with a notification in the victim's browser.

## Open Questions

None (anonymous-realtime decision inherited from 0013).

## Out of Scope

- Reservation creation/flow (0024). Zoom/pan for huge rooms. Seat
  recommendations.
