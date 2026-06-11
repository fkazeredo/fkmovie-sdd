# 0003 - Authentication

Status: Implemented
Related ADRs: 0005, 0003

> Implementation notes (backend, this milestone):
> - WebSocket CONNECT JWT validation is deferred to spec 0013 (no STOMP transport
>   exists yet). The reusable seam is the `JwtDecoder` bean in
>   `com.fksoft.infra.security`, to be injected into the future CONNECT
>   `ChannelInterceptor`.
> - `auth.invalid-request` (400) is delivered as the foundation's global
>   `validation.error` contract (spec 0001) with the `fields` list, instead of a
>   separate code — one validation contract across the API.
> - Per-IP rate limit threshold set to 20 failures / 15 min (spec left the number open).
> - `users.email` is `CITEXT` (DB-level case-insensitive uniqueness) AND normalized to
>   lowercase by the application — redundant by design, defense in depth.
> - Change-password revokes all of the user's refresh tokens (other sessions must
>   re-authenticate).
> - Angular login/registration UI is spec 0021.

## Goal

Provide JWT-based authentication for all three roles (`CUSTOMER`,
`OPERATOR`, `ADMIN`). Users authenticate with email and password, receive a
short-lived access token and a long-lived refresh token, and can log out
revoking the refresh.

## Scope

Login, refresh, logout, password change. User creation is in 0004 (customer
self-registration) and 0005 (admin-invited operators/admins). Password reset
is in 0004 (it uses the same email infrastructure as registration).

## Business Context

The system is closed for reservation operations — every reservation belongs
to an identified user. Customers self-register; operators and admins are
invited. Roles drive authorization in business endpoints.

## Business Rules

- Authentication MUST use the two-token pattern of ADR 0005 (access JWT 15 min,
  refresh opaque 7 days, single-use refresh).
- Passwords MUST be stored with bcrypt cost 12 (Spring Security default).
- A user MUST have exactly one active role at a time
  (`CUSTOMER | OPERATOR | ADMIN`). Roles do not stack in v1.
- A user MUST have a status (`ACTIVE | DISABLED`). Disabled users cannot log
  in. Disabling does not delete the user (preserves history).
- Login MUST be rate-limited: 5 failed attempts per email per 15 min trigger
  a 15-min lockout. Rate also applied per IP for abuse.
- Email is the username and MUST be unique (case-insensitive, normalized to
  lowercase at storage).
- All passwords MUST satisfy: 8+ chars, at least one letter and one digit.
  No other complexity rules (per OWASP modern guidance).
- Refresh token reuse (a revoked refresh used again) MUST revoke all the
  user's refresh tokens (suspected token theft).
- Logout MUST revoke the refresh token presented. The access token cannot
  be invalidated server-side before its `exp` (accepted trade-off, ADR 0005).
- WebSocket CONNECT frame MUST carry the access token in the `Authorization`
  header equivalent (STOMP header `Authorization: Bearer <jwt>`). Server
  validates and binds principal.

## Input/Output Examples

**Login success**:

```http
POST /api/auth/login
{ "email": "user@example.com", "password": "secret123" }
```

```http
HTTP/1.1 200 OK
Set-Cookie: refreshToken=<opaque>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800
{
  "accessToken": "<jwt>",
  "accessTokenExpiresAt": "2026-06-10T12:15:00Z",
  "user": { "id": "uuid", "email": "user@example.com", "name": "Frank",
            "role": "CUSTOMER" }
}
```

**Refresh**:

```http
POST /api/auth/refresh
Cookie: refreshToken=<opaque>
→ same shape as login response (new access + new refresh in cookie)
```

## API Contracts

- `POST /api/auth/login` — issues access + refresh.
- `POST /api/auth/refresh` — exchanges refresh cookie for a new pair.
- `POST /api/auth/logout` — revokes presented refresh; clears cookie.
- `POST /api/auth/change-password` — authenticated; old + new password.
- `GET /api/auth/me` — authenticated; returns current user summary.

All endpoints are public to `auth/*` paths except `me` and `change-password`.

## Events

- `UserLoggedIn(userId, ip, userAgent, occurredAt)` — for audit/observability.
- `UserLoggedOut(userId, occurredAt)`.
- `PasswordChanged(userId, occurredAt)`.

Internal events (`@TransactionalEventListener`). Not exposed externally in v1.

## Persistence Changes

Tables:

- `users(id UUID PK, tenant_id, email CITEXT UNIQUE, password_hash, name,
   role, status, email_verified_at, created_at, updated_at)` — `tenant_id`
   per ADR 0003 (default value `default`).
- `refresh_tokens(id UUID PK, user_id FK, token_hash, expires_at,
   revoked_at, replaced_by_id NULL, created_at, ip, user_agent)` — token
   stored hashed; reuse detection by `revoked_at != null AND used again`.
- `login_attempts(id, email, ip, succeeded, attempted_at)` for rate limit.

Indexes: `users(email)`, `refresh_tokens(user_id)`,
`login_attempts(email, attempted_at)`, `login_attempts(ip, attempted_at)`.

## Validation Rules

- Delivery boundary: email and password presence/format via Bean Validation.
- Application boundary: existence of user, active status, password match.
- Domain boundary: invariant — role and status are required and from a
  closed set (enums).

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 400 | `auth.invalid-request` | Missing/invalid fields. |
| 401 | `auth.invalid-credentials` | Wrong email/password. |
| 401 | `auth.invalid-refresh` | Refresh missing/expired/revoked. |
| 401 | `auth.token-reuse-detected` | Revoked refresh reused (all tokens revoked). |
| 403 | `auth.disabled` | User exists but is DISABLED. |
| 429 | `auth.rate-limited` | Lockout active. Response includes `Retry-After`. |
| 401 | `auth.password-mismatch` | Change-password with wrong old password. |
| 401 | `auth.unauthenticated` | Missing/invalid access token on a protected endpoint. |
| 403 | `auth.forbidden` | Authenticated but lacking the required authority. |
| 400 | `validation.error` | Missing/invalid fields (replaces `auth.invalid-request`; global contract from spec 0001). |

User-facing messages are i18n keys; the API also returns a stable `message`
field localized by `Accept-Language` (pt-BR / en).

## Observability Requirements

- Log every login attempt with `email` (hashed in logs to avoid PII storage
  in log aggregator), `ip`, `userAgent`, `outcome`. Never log password.
- Metrics: `auth_login_total{outcome=...}`, `auth_refresh_total`,
  `auth_logout_total`, `auth_rate_limit_triggered_total`.
- Alert (operational, not in v1): repeated `token-reuse-detected` for the
  same user is a security signal.

## Tests Required

- Unit: password hashing, JWT issuance, refresh single-use rule,
  reuse-detection cascade revocation.
- Integration: full login → refresh → logout flow with Testcontainers.
- Integration: rate limit triggers 429 after N failures and resets.
- Integration: disabled user gets 403.
- Integration: WebSocket CONNECT with valid/invalid JWT.
- Architectural: `auth` module exposes only its public facade; no other
  module accesses `users` table directly.

## Acceptance Criteria

- Login returns access + refresh; access works on protected endpoints.
- Refresh rotates tokens; reusing an old refresh revokes all.
- Logout invalidates the refresh.
- Rate limit returns 429 with `Retry-After`.
- WebSocket connection rejected without valid JWT in CONNECT.

## Open Questions

- `login_attempts` retention: rows accumulate indefinitely (window queries are
  index-served regardless of size). A scheduled cleanup/retention job is deferred.
- Reset-on-success semantics: a successful login does not reset the failure window;
  failures within the last 15 min still count toward lockout (literal reading).
- Password reset flow lives in spec 0004 (uses email infra from 0006).
- Should we enforce email verification before allowing reservation? Default
  proposal: yes (decided in 0004).
- Multi-factor authentication: out of scope for v1.

## Out of Scope

- User creation (0004, 0005).
- Password reset by email (0004).
- Email verification (0004).
- OAuth/SSO (future).
- MFA (future).
