# 0026 - Angular Admin Console and Operator Screens

Status: Draft
Related ADRs: 0008

## Goal

Back-office UI: admins manage movies (0008), screenings (0009), pricing
(0012) and users (0005); operators search reservations and reprint tickets
(0020).

## Scope

`features/admin` and `features/operator`, both behind `roleGuard`. PrimeNG
DataTable-centric. This is deliberately utilitarian UI — product polish
budget goes to the customer flows.

## UI Behavior

**Shell**: `/admin` (ADMIN) and `/operator` (OPERATOR or ADMIN) with a
sidebar layout (PrimeNG), language switcher, current-user menu with logout.

**Movies** (`/admin/movies`): DataTable with search/status filter/
pagination wired to 0008; create/edit in a Dialog form (title, duration,
age rating select with BR badges, synopsis, posterUrl with preview);
archive/unarchive with ConfirmDialog; delete enabled only when the API
allows (409 `movie.has-screenings` surfaced as a tooltip-explained
disable after first attempt). Duplicate-title warning (non-blocking) on
save.

**Screenings** (`/admin/screenings`): filterable table (room, movie, date
range); create/edit form: movie autocomplete (ACTIVE only), room select,
date-time picker (America/Sao_Paulo input converted to UTC for the API —
the form is explicit about the timezone), base price input in BRL; API
errors mapped: `room-overlap` highlights the conflicting time,
`has-reservations` explains why editing is locked; cancel action gated by
`has-sold-tickets`. Detail shows free/held/sold counts.

**Pricing** (`/admin/pricing`): two editable tables (seat-type surcharges,
weekday multipliers) with inline validation mirroring 0012 (ACCESSIBLE/
COMPANION locked at 0), save with diff confirmation.

**Users** (`/admin/users`): table with role/status filters; invite dialog
(email, name, role); role change, disable/enable with the 0005 guards
surfaced (`cannot-disable-self`, `cannot-disable-last-admin` as disabled
actions with tooltips); resend invitation with rate-limit feedback.

**Operator** (`/operator`): single search screen — one input + criterion
selector (ticket code / reservation id / email) per 0020; results table;
detail drawer with full reservation; "Reimprimir" opens a print-friendly
route (`/operator/tickets/:id/print`) that renders the ticket (code,
seat, movie, room, time, age rating) with `@media print` styling and
triggers `window.print()`; reprint count badge.

All mutations: optimistic-free (server-confirmed), toast on success,
mapped i18n errors, audit-relevant actions need no extra UI (backend
audits).

## Architecture

`features/admin/{movies,screenings,pricing,users}` and
`features/operator`, each with `data-access`, `pages`, `ui`, `models`.
Shared admin table patterns extracted into `features/admin/ui` (not
app-level `shared` until a third consumer exists — per shared-code rule).

## Tests Required

- Guards: role matrix for /admin and /operator.
- Movies/screenings forms: validation, error mapping (overlap,
  has-reservations), timezone conversion correctness (the most bug-prone
  spot — test SP↔UTC both directions incl. DST-less current rules).
- Pricing: locked rows, range validation.
- Users: guard-driven disabled actions.
- Operator: criterion exclusivity, print route renders ticket data.

## Acceptance Criteria

- An admin schedules tomorrow's screening in under a minute; an operator
  finds a ticket by code and prints it; all in PT-BR or EN.

## Open Questions

- Operator print: thermal-printer column width constraints? v1 assumes A4/
  letter browser print; confirm counter hardware later.

## Out of Scope

- Dashboards/analytics. Mass operations. Screening cancellation with
  refunds (blocked by 0009 scope).
