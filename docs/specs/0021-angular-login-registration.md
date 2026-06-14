# 0021 - Angular Login and Registration

Status: Implemented
Related ADRs: 0008, 0005

## Goal

Frontend flows for spec 0003/0004: login, registration, email verification,
forgot/reset password, session handling with silent refresh, role-based
routing.

## Scope

`core/auth` (token store, interceptor, guards, refresh logic) and
`features/auth` (pages). Backend contracts are 0003/0004 — no invention.

## UI Behavior

- Routes: `/login`, `/register`, `/verify-email` (token from query),
  `/forgot-password`, `/reset-password` (token from query).
- Login form: email + password; loading state on submit; backend errors
  shown via i18n key mapping (`auth.invalid-credentials`,
  `auth.rate-limited` with retry countdown from `Retry-After`,
  `auth.disabled`).
- Registration: name, email, password + confirmation; on 201, auto-login
  (per 0004 decision) and banner "verify your email" until verified.
- Verify-email page consumes the token on load and shows success/expired
  (410 → offer resend, rate-limit aware).
- Access token kept in memory only (signal in `core/auth`); refresh cookie
  is httpOnly (browser-managed). On app bootstrap, attempt silent
  `POST /api/auth/refresh` to restore the session.
- HTTP interceptor: attaches `Authorization` when a token exists; on 401
  (except auth endpoints), performs ONE refresh attempt and retries the
  original request; concurrent 401s share the same refresh promise; on
  refresh failure, clears session and redirects to `/login` with
  `returnUrl`.
- Guards: `authGuard` (any authenticated), `roleGuard(CUSTOMER|OPERATOR|
  ADMIN)`. Post-login redirect by role: CUSTOMER → `returnUrl` or
  `/screenings`; OPERATOR → `/operator`; ADMIN → `/admin`.
- Logout calls the backend, clears memory state, navigates to
  `/screenings`.
- All texts via ngx-translate (PT-BR + EN).

## Architecture

`core/auth`: `auth.service.ts` (signals: `currentUser`, `isAuthenticated`),
`token.store.ts`, `auth.interceptor.ts`, `guards.ts`.
`features/auth`: `pages/` (login, register, verify, forgot, reset), `ui/`
(form components), `data-access/auth-api.service.ts`, `models/`.
PrimeNG forms + Tailwind layout. Reactive Forms with typed forms.

## Tests Required

- `auth.service`: login/logout state transitions; refresh-on-bootstrap.
- Interceptor: 401 → single shared refresh → retry; refresh failure →
  redirect; no refresh loop on `/api/auth/*` 401s.
- Guards: role routing matrix.
- Pages: validation states, backend error mapping (component tests with
  mocked API), rate-limit countdown rendering.

## Acceptance Criteria

- Register → email verify (dev Gmail) → login → land on screenings;
  refresh of the browser restores the session silently; expired refresh
  lands on `/login`.

## Open Questions

- "Remember me" (longer refresh)? v1: no — fixed 7-day refresh from 0003.

## Out of Scope

- Profile editing, MFA, social login, password strength meter beyond the
  0003 rule.

## Implementation decisions

- **Routes are Portuguese**, for consistency with the already-shipped
  public routes (`/sessoes`) and the pt-BR default: `/login`, `/cadastro`.
  **Exception — `/verify-email` stays English** because the backend builds
  that exact link (`NotificationEventListener` →
  `${APP_BASE_URL}/verify-email?token=…`); the path is a backend↔frontend
  contract, not UX. A `/verificar-email` redirect alias is kept. (A routes
  contract test, `app.routes.spec.ts`, guards this.) Post-login redirect:
  CUSTOMER → `returnUrl` or `/sessoes`; OPERATOR → `/operador`; ADMIN →
  `/admin`.
- **Email-not-verified banner** lives in the app shell
  (`core/layout/verify-email-banner`) and offers a one-click resend, instead
  of a per-page banner — it follows the user across the app until verified.
- **Rate-limit live countdown deferred.** `auth.rate-limited` /
  `user.rate-limited` show a static "try again shortly" message; the
  `Retry-After` header is not yet surfaced (the `ApiError` model does not
  carry response headers). Tracked here as a follow-up; no behavior change to
  the backend contract.
- **Forgot/reset password deferred** to a later slice — not on the
  register → verify → login → reserve critical path. The backend endpoints
  (`/api/users/forgot-password`, `/reset-password`) already exist.
- Backend localizes error `message`; the frontend overrides known `code`s via
  `errors.<code>` i18n keys (`core/http/error-text.ts`) and falls back to the
  backend message otherwise.

## Status notes

Implemented in `core/auth` (model, token store, service with shared silent
refresh, bearer+401-retry interceptor, `authGuard`/`roleGuard`) and
`features/auth` (login, register, verify-email pages). Tests: auth.service
(login/register/logout/bootstrap/shared-refresh/verify), interceptor
(bearer, single-refresh-retry, refresh-fail redirect, no auth-loop), guards
(role matrix), and the three pages (validation + backend error mapping).
Frontend suite green (54 tests).
