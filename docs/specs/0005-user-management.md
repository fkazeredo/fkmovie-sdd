# 0005 - User Management (Admin)

Status: Implemented
Related ADRs: 0005

> Implementation notes (backend):
> - First-admin bootstrap resolved (Open Question): an `ApplicationRunner` creates
>   one ACTIVE admin from `BOOTSTRAP_ADMIN_EMAIL`/`BOOTSTRAP_ADMIN_PASSWORD` at
>   startup if no admin exists; idempotent afterwards.
> - Role change does NOT revoke active tokens (Open Question resolved → no; they
>   expire within 15 min).
> - Error codes `auth.unauthorized`/`auth.forbidden` in the table are served by the
>   existing `auth.unauthenticated` (401) / `auth.forbidden` (403) from spec 0003.
> - Management lives inside the `auth` module (same `users` aggregate); admin
>   endpoints are gated by `ROLE_ADMIN` URL rules in SecurityConfig.
> - The "cannot disable the last admin" rule is unreachable in normal flow (the
>   acting admin is itself active, so a second active admin always remains) — it
>   only triggers for a disabled admin acting on a still-valid token; covered by a
>   unit test on `UserManagementPolicy`.
> - Angular admin console UI is spec 0026.

## Goal

Admins create and manage internal users (operators and other admins) via an
invitation flow. Self-registration is reserved for customers (spec 0004).

## Scope

CRUD operations on users restricted to the `ADMIN` role: list, view, invite,
update role, disable/re-enable, resend invitation.

## Business Context

The cinema operator's staff (ticket counter, management) are not customers
and do not register themselves. An admin creates them with a role and they
receive an invitation email with a link to set their password.

## Business Rules

- Only `ADMIN` users may access this API.
- Inviting a user creates the user with `status = DISABLED`,
  `email_verified_at = NULL`, and `password_hash = NULL` (cannot log in).
  An invitation token is sent by email (24h TTL).
- The invitee accepts the invitation: validates token, sets password and
  name (name may also have been set by the admin); on success
  `status = ACTIVE`, `email_verified_at` is set (clicking the email
  link implicitly verifies), password is stored hashed.
- Inviting an email that already exists MUST return 409.
- Admin can change another user's role between `OPERATOR` and `ADMIN`.
  Demoting to `CUSTOMER` is allowed but UI-discouraged (admin role
  conversion is unusual). Customers (`role = CUSTOMER`) created via
  self-registration are also visible in admin listing.
- Admin MUST NOT disable their own account. An admin cannot disable the
  last active admin (system would be unmanageable).
- Re-inviting (resend invitation) is allowed while the user is still
  `DISABLED`, rate-limited to 1 per 5 minutes per target user.

## Input/Output Examples

**Invite operator**:

```http
POST /api/admin/users
Authorization: Bearer <admin-jwt>
{ "email": "op1@cinema.com", "name": "Maria", "role": "OPERATOR" }
```

```http
201 Created
{ "id": "uuid", "email": "op1@cinema.com", "name": "Maria",
  "role": "OPERATOR", "status": "DISABLED", "invitedAt": "..." }
```

**Accept invitation** (public — uses one-time token):

```http
POST /api/users/accept-invitation
{ "token": "<opaque>", "password": "newSecret123", "name": "Maria Silva" }
→ 200 same shape as login response (auto-login on accept).
```

## API Contracts

- `POST /api/admin/users` — admin only, invite.
- `GET /api/admin/users?role=&status=&page=&size=` — admin only, paginated.
- `GET /api/admin/users/{id}` — admin only.
- `PUT /api/admin/users/{id}/role` — admin only.
- `POST /api/admin/users/{id}/disable` — admin only.
- `POST /api/admin/users/{id}/enable` — admin only.
- `POST /api/admin/users/{id}/resend-invitation` — admin only.
- `POST /api/users/accept-invitation` — public.

## Events

- `UserInvited(userId, email, role, invitedByAdminId, occurredAt)`.
- `UserInvitationAccepted(userId, occurredAt)`.
- `UserRoleChanged(userId, oldRole, newRole, changedByAdminId, occurredAt)`.
- `UserDisabled(userId, disabledByAdminId, occurredAt)`.
- `UserEnabled(userId, enabledByAdminId, occurredAt)`.

## Persistence Changes

Tables:

- Reuse `users` from 0003 — add nullable `invited_by_user_id` FK and
  `invited_at`.
- `invitation_tokens(id, user_id FK, token_hash, expires_at, consumed_at,
   created_at)`.

## Validation Rules

- Email validated as in 0004.
- Role from closed enum.
- Authorization: every endpoint requires `ROLE_ADMIN` (Spring Security
  expression).
- Business: cannot disable self, cannot disable last admin (policy class
  `UserManagementPolicy`).

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 401 | `auth.unauthorized` | No token. |
| 403 | `auth.forbidden` | Token belongs to non-admin. |
| 409 | `user.email-taken` | Invitation email already exists. |
| 409 | `user.cannot-disable-self` | Admin tried to disable own account. |
| 409 | `user.cannot-disable-last-admin` | Would leave no active admin. |
| 410 | `user.token-expired` | Invitation token expired/consumed. |
| 404 | `user.not-found` | Unknown user id. |

## Observability Requirements

- Log every admin action with `actingUserId`, `targetUserId`, `action`,
  `outcome`. These are business-relevant audit entries.
- Metrics: `admin_users_invited_total`, `admin_users_role_changed_total`,
  `admin_users_disabled_total`.

## Tests Required

- Integration: admin invites, target accepts, can log in.
- Integration: non-admin gets 403 on every admin endpoint.
- Integration: cannot disable self, cannot disable last admin.
- Integration: accept-invitation with expired token returns 410.

## Acceptance Criteria

- An admin can invite an operator who receives the email and signs in after
  accepting.
- Role changes apply on next login (existing tokens still carry the old role
  until they expire, max 15 min; reset via revoke if urgent).
- Audit trail visible in logs.

## Open Questions

- Bootstrap: resolved — an `ApplicationRunner` (not a Flyway seed, which cannot
  bcrypt) creates the first admin from `BOOTSTRAP_ADMIN_EMAIL`/
  `BOOTSTRAP_ADMIN_PASSWORD` if no admin exists; idempotent thereafter.
- Role-change token revocation: resolved — no (tokens expire within 15 min). A
  dedicated revoke endpoint remains out of scope for v1.
- Invitation-token cleanup job (consumed/expired): deferred, same posture as the
  other token tables (spec 0004).

## Out of Scope

- Per-user fine-grained permissions (only role-based in v1).
- Audit history table (logs are the audit trail; LGPD is baseline per ADR).
- Bulk operations.
