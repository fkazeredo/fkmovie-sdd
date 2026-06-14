# 0025 - Angular My Reservations and Cancellation

Status: Implemented
Related ADRs: 0008

## Goal

Customers manage their history: list reservations (0019), open details,
cancel within the rules (0018), and reach tickets again.

## Scope

`features/reservation` additions: list page, detail reuse, cancellation
interaction.

## UI Behavior

- Route: `/me/reservations` (authGuard, CUSTOMER).
- Tabs/filters: "Próximas" (upcoming=true), "Todas", by status chips.
  PrimeNG table or card list; pagination per 0019 envelope.
- Row: movie, date/time (locale + America/Sao_Paulo), seats, total,
  status badge (consistent color mapping with 0024).
- Detail navigates to `/reservations/:id` (0024 page — single
  status-driven page serves both flows).
- Cancel button visible when status is PENDING/AWAITING_PAYMENT, or
  CONFIRMED with `startsAt - now >= 2h` (client-side gate mirrors 0018;
  backend remains authority — a 409 `cancellation-window-closed` is
  handled gracefully if the window closes between render and click).
- Cancellation uses PrimeNG ConfirmDialog: shows seats, total, and for
  CONFIRMED the refund notice (i18n placeholder pending owner copy, 0018
  open question). On success: toast + status refresh; refund summary shown
  on the detail.
- Empty state with CTA to `/screenings`.

## Architecture

Reuses `reservations-api.service.ts` (adds list call) and the 0024 page.
`pages/my-reservations.page.ts` with signals for filters/pagination.

## Tests Required

- List rendering, filters, pagination; status badge mapping.
- Cancel visibility matrix incl. the 2h boundary (mocked clock/now).
- Cancel flow: confirm dialog → API → refreshed state; 409 window-closed
  handled with message.

## Acceptance Criteria

- A customer cancels an eligible confirmed reservation from the list in
  ≤3 interactions and sees the refund acknowledgment.

## Open Questions

- Inherits the refund-copy question from 0018/0024.

## Out of Scope

- Receipts/exports. Re-booking shortcuts.

## Implementation decisions

- Route is `/minhas-reservas` (Portuguese, matches the header link), guarded
  by `authGuard`. Detail reuses the SPEC-0024 page at `/reservas/:id`.
- Filters: a Próximas/Todas scope toggle plus status chips (one per
  `ReservationStatus` + "all"). Pagination via the SPEC-0019 envelope
  (10/page, prev/next).
- Cancel is gated client-side by the SPEC-0018 window (PENDING/
  AWAITING_PAYMENT always; CONFIRMED only when `startsAt − now ≥ 2h`), with
  the backend as authority — a `booking.cancellation-window-closed` 409 is
  caught, shown as an error toast, and the list refreshed. Confirmation uses
  PrimeNG ConfirmDialog; success shows a refund-aware toast.
- Refund copy uses placeholder i18n (`minhasReservas.refundAck`) pending the
  owner's final wording (inherited open question from 0018/0024).

## Status notes

Implemented as `features/reservation/pages/my-reservations-page` reusing
`reservations-api.service` (added `list`). Tests: list rendering, empty
state, scope/status filter reloads, the cancel-window matrix, the
cancel→refund-ack→refresh flow and the 409 window-closed path. Frontend
suite green (78 tests).
