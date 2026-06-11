# 0024 - Angular Reservation Flow

Status: Draft
Related ADRs: 0008, 0006

## Goal

From a seat selection to tickets on screen: create the hold (0014), show
the summary with a live countdown, confirm (0016), ride the async payment
via the user realtime queue, and present tickets or failure.

## Scope

`features/reservation`: creation handoff from 0023, summary page,
confirmation, payment-pending state, result states.

## UI Behavior

- "Reservar" in 0023 POSTs the reservation (0014 contract). On 201 →
  navigate `/reservations/:reservationId`. Backend rejections map to i18n
  messages: `booking.seats-unavailable` returns the user to the seat map
  with the stolen seats highlighted; `booking.active-reservation-exists`
  offers a link to the existing reservation;
  `user.email-not-verified` offers resend-verification.
- Summary page states by reservation status:
  - `PENDING`: seats + prices + total; **countdown to `expiresAt`** —
    server-anchored: remaining = `expiresAt - (now + serverOffset)`, where
    `serverOffset` is estimated from the response `Date` header; recomputed
    on tab focus (resists client clock skew). On reaching zero → switch to
    expired state (and the realtime/refetch confirms). Buttons: "Confirmar
    compra" and "Cancelar" (0018).
  - `AWAITING_PAYMENT` (after confirm 202): processing state with spinner
    + payment deadline countdown; subscribed to `/user/queue/reservations`;
    on `CONFIRMED` → success state; on `CANCELLED` → failure state with
    "tentar novamente" (re-reserve — old hold is gone) guidance. Fallback:
    poll `GET /api/reservations/{id}` every 5 s in case the socket drops.
  - `CONFIRMED`: success — tickets list (code, seatLabel) with print-
    friendly styling, "ver minhas reservas" link.
  - `EXPIRED` / `CANCELLED`: clear message + path back to the seat map.
- Refreshing the page at any point restores the correct state from the GET
  endpoint (status-driven rendering, no client-only state machine).
- Confirm button disabled while in-flight; re-click safe (0016 idempotent).

## Architecture

`data-access/reservations-api.service.ts`,
`pages/reservation.page.ts` (status-driven signal state; countdown via
`interval` + computed), `ui/` (summary-card, countdown, tickets-panel,
status-banner). Realtime subscription through `core/realtime` (0023).

## Tests Required

- Status-driven rendering matrix (PENDING/AWAITING/CONFIRMED/EXPIRED/
  CANCELLED) from GET payloads.
- Countdown: server-offset math, tab-focus resync, zero → expired state.
- Realtime CONFIRMED/CANCELLED handling + polling fallback.
- Error mapping on create (seats-unavailable → back with highlights).

## Acceptance Criteria

- Demo: select 2 seats (1 meia) → reserve → confirm → ~3 s spinner →
  tickets on screen and email received; killing the WebSocket mid-payment
  still resolves via polling.

## Open Questions

- Refund SLA copy (from 0018) appears in cancellation confirmations here
  and in 0025 — owner to provide final wording (placeholder i18n keys).

## Out of Scope

- Card forms (mock gateway needs none). My-reservations list (0025).
