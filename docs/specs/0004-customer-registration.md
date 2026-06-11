# 0004 - Customer Registration, Email Verification and Password Reset

Status: Implemented
Related ADRs: 0005, 0007

> Implementation notes (backend, bundled with spec 0006):
> - Registration auto-logs the customer in (Open Question resolved → yes): POST
>   /register returns 201 with accessToken + refresh cookie, same shape as login,
>   and `user.emailVerified=false`.
> - `preferred_locale` added to `users` (V3 migration), captured from
>   `Accept-Language` (pt-BR default); used for email locale.
> - `user.invalid-request` (400) is delivered as the foundation's global
>   `validation.error` contract (spec 0001) with `fields`.
> - Registration lives inside the `auth` module (same `users` aggregate, reuses
>   token issuer + refresh revocation) rather than a separate module.
> - Reservation gate (reject `email_verified_at` NULL) is enforced from spec 0014.
> - Angular UI is spec 0021.

## Goal

Customers self-register, verify their email, and can reset a forgotten
password through a secure email link. Operators and admins are NOT created
through this flow (see 0005).

## Scope

Self-service flows for the `CUSTOMER` role: register, verify email,
forgot-password, reset-password. Uses email infrastructure from 0006.

## Business Context

The customer must have a verified email before reserving (so reservation
confirmation emails can reach them). Verification is one-time; reset can
happen multiple times.

## Business Rules

- A new customer self-registers with `name`, `email`, `password`. Role is
  always `CUSTOMER`. Status is `ACTIVE` on creation but `email_verified_at`
  is NULL until verified.
- Reservation creation (spec 0014) MUST reject users whose
  `email_verified_at` is NULL.
- A verification email MUST be sent on registration with a one-time link
  containing a signed, time-limited token (24 hours).
- Verification link is single-use; on successful verification,
  `email_verified_at` is set. Subsequent use of the same link returns 410
  Gone.
- Resending the verification email MUST be possible while
  `email_verified_at` is NULL, rate-limited to 1 per minute per user.
- Forgot-password requests MUST send a reset link with a signed,
  time-limited token (1 hour), single-use. Always respond with 200 even
  when the email is unknown, to avoid account enumeration.
- Password reset uses the same complexity rules as spec 0003. Resetting
  password MUST revoke all refresh tokens for that user (force re-login on
  all devices).
- Email already registered MUST return 409 with code `user.email-taken`.
- Tokens MUST be stored hashed; the raw token only appears in the email.

## Input/Output Examples

**Register**:

```http
POST /api/users/register
{ "name": "Frank", "email": "frank@example.com", "password": "secret123" }
```

```http
201 Created
{ "id": "uuid", "email": "frank@example.com", "name": "Frank",
  "emailVerified": false }
```

**Verify**:

```http
POST /api/users/verify-email
{ "token": "<opaque>" }
→ 200 { "email": "frank@example.com", "emailVerified": true }
```

## API Contracts

- `POST /api/users/register` — public.
- `POST /api/users/resend-verification` — public (rate-limited by email).
- `POST /api/users/verify-email` — public.
- `POST /api/users/forgot-password` — public, always 200.
- `POST /api/users/reset-password` — public, takes token + new password.

## Events

- `CustomerRegistered(userId, email, occurredAt)` — triggers verification
  email.
- `EmailVerified(userId, occurredAt)`.
- `PasswordResetRequested(userId, email, occurredAt)` — triggers reset
  email.
- `PasswordResetCompleted(userId, occurredAt)` — triggers token revocation.

Internal events via `@TransactionalEventListener(AFTER_COMMIT)`. Email
delivery is async, never fails the business transaction.

## Persistence Changes

Tables:

- Reuse `users` from 0003.
- `email_verification_tokens(id, user_id FK, token_hash, expires_at,
   consumed_at, created_at)`.
- `password_reset_tokens(id, user_id FK, token_hash, expires_at,
   consumed_at, created_at, requested_ip)`.

Indexes on `user_id` and `token_hash`. Expired/consumed tokens cleaned up by
a daily job (kept 30 days for audit, then purged).

## Validation Rules

- Email format via Bean Validation `@Email`.
- Password 8+ chars, ≥1 letter, ≥1 digit.
- Name 2–100 chars, trimmed, no control characters.

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 400 | `user.invalid-request` | Invalid fields. |
| 409 | `user.email-taken` | Email already registered. |
| 410 | `user.token-expired` | Verification or reset token expired/consumed. |
| 400 | `user.token-invalid` | Token signature/format invalid. |
| 429 | `user.rate-limited` | Resend/forgot abuse. |

`forgot-password` returns 200 even on unknown email (account-enumeration
defense). Internally still emits a metric for ops visibility.

## Observability Requirements

- Log `register`, `verify-email`, `forgot-password`, `reset-password`
  outcomes with `userId` when available.
- Metrics: `users_registered_total`, `users_email_verified_total`,
  `users_password_reset_total`, `users_token_expired_total`.

## Tests Required

- Unit: token generation, hashing, expiry check, single-use rule.
- Integration: full register → verify → login flow.
- Integration: register-then-register with same email returns 409.
- Integration: verify expired/consumed token returns 410.
- Integration: forgot-password with unknown email returns 200.
- Integration: reset-password revokes all refresh tokens.

## Acceptance Criteria

- Customer can register and receive verification email in dev (Gmail SMTP
  per ADR 0007).
- Verification activates the account for reservation flows.
- Forgot/reset flow completes end-to-end.
- Reservation creation (when 0014 lands) rejects unverified emails.

## Open Questions

- Token TTLs (24h verification, 1h reset) — confirmed. Owner can shorten.
- Auto-login on registration — resolved: yes (see implementation note).
- Daily cleanup job for expired/consumed tokens (kept 30 days then purged):
  deferred. Window/single-use checks do not depend on it; tables grow until a
  retention job is added.

## Out of Scope

- Admin/operator user creation (0005).
- Profile editing beyond password (future).
- Account deletion / right to be forgotten (future — design preserves
  soft-delete via `users.status = DISABLED`).
- Social login.
