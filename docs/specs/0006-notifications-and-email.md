# 0006 - Notifications and Email

Status: Implemented
Related ADRs: 0007

> Implementation notes:
> - Outbox + @Scheduled dispatcher (not @Async) for crash-safe, restartable
>   delivery. Backoff 1m/5m/15m/1h/6h/24h; FAILED_PERMANENT after the 6th failure.
> - Only VERIFICATION and PASSWORD_RESET templates ship now; other templates
>   arrive with their producing specs (Rule Zero).
> - `preferred_locale` (Open Question) added to `users` in spec 0004's V3 migration.
> - Cleanup of SENT/FAILED_PERMANENT rows is deferred (manual in v1).
> - `spring.messages.fallback-to-system-locale=false` so an EN user never receives
>   a PT email regardless of the server locale.

## Goal

Provide a reliable, async email delivery mechanism that other modules use to
notify users (verification, reset, invitation, purchase confirmation,
cancellation, ticket delivery). Configuration follows the layered strategy
of ADR 0007 (Gmail SMTP in dev, transactional provider in prod). Failure to
send MUST NOT fail the originating business transaction.

## Scope

The `notification` module: `EmailSender` port and Spring Mail-based
implementation, outbox table, async dispatch, retry, dead-letter, templates,
i18n. No SMS or push in v1.

## Business Context

Several flows generate user-facing emails. Email delivery latency is
unimportant (seconds, not real-time). Reliability matters more than speed:
a confirmation email lost is a support call.

## Business Rules

- Other modules publish a domain event (e.g., `CustomerRegistered`) and the
  notification module reacts via `@TransactionalEventListener(AFTER_COMMIT)`
  to enqueue the email — guaranteeing the source transaction has committed.
- Emails MUST be persisted in the `outbox_emails` table before send is
  attempted. The send worker reads from the outbox.
- Send failures MUST be retried with exponential backoff: 1m, 5m, 15m,
  1h, 6h, 24h. After the 6th failure, the email is moved to dead-letter
  status (`FAILED_PERMANENT`).
- Templates use `MessageSource` keys + a templating engine (Thymeleaf is
  the natural choice in Spring). HTML body + plain-text fallback.
- Locale resolution: prefer the user's stored language preference; fall
  back to `pt-BR`. (User language preference column added in 0004 as a
  follow-up — for now use `pt-BR`.)
- `MAIL_FROM` is an env var; `MAIL_TO_OVERRIDE` (optional) redirects ALL
  outbound mail to a single address — for dev/staging safety. Production
  MUST NOT set `MAIL_TO_OVERRIDE`.
- Secrets (SMTP password / API key) come from env vars; never logged.

## Input/Output Examples

Not applicable directly — this is infrastructure. Example usage from
another module:

```java
applicationEvents.publishEvent(
    new CustomerRegistered(userId, email, name, occurredAt));
// In notification module:
@TransactionalEventListener(phase = AFTER_COMMIT)
public void on(CustomerRegistered evt) {
    outbox.enqueue(EmailTemplate.VERIFICATION,
                   evt.email(), Map.of("name", evt.name(), ...));
}
```

## API Contracts

No external REST API. Internal port:

```java
public interface EmailSender {
    void send(EmailMessage message);  // throws EmailSendException
}
```

## Events

Consumes (listed, not exhaustive — each producing spec declares):

- `CustomerRegistered` → verification template.
- `PasswordResetRequested` → reset template.
- `UserInvited` → invitation template.
- `ReservationConfirmed` → purchase confirmation + tickets.
- `ReservationCancelled` → cancellation notice / refund confirmation.
- `ReservationExpired` → expiration notice (optional, decided in 0017).

Publishes:

- `EmailSent(emailId, recipient, template, occurredAt)`.
- `EmailPermanentlyFailed(emailId, recipient, template, lastError, occurredAt)`.

## Persistence Changes

Tables:

- `outbox_emails(id UUID PK, tenant_id, recipient_email, template_key,
   locale, payload_json, status, attempts, next_attempt_at, last_error,
   created_at, sent_at)` where `status ∈ {PENDING, SENT, FAILED_PERMANENT}`.
- Indexes: `(status, next_attempt_at)` for the worker's polling query;
  `(created_at)` for cleanup.

Cleanup: `SENT` emails older than 90 days are deleted. `FAILED_PERMANENT`
older than 1 year are archived (manual operation in v1).

## Validation Rules

- Recipient email format check before enqueue. Malformed emails are rejected
  by the producing module — outbox assumes validity.
- Template key must exist in the template registry; unknown keys throw at
  enqueue time (developer error).

## Error Behavior

For consumers of the notification module: enqueueing is best-effort and
silent. A persistent failure in the producing transaction would prevent the
enqueue — but the source business event has not committed in that case
either, so the system is consistent.

Send errors are classified:

- Transient (SMTP timeout, 4xx rate limit) → retry with backoff.
- Permanent (invalid recipient, 5xx unrecoverable) → `FAILED_PERMANENT`.

## Observability Requirements

- Metrics: `email_outbox_size{status=...}` (gauge),
  `email_send_attempts_total{outcome=...}`, `email_send_latency_seconds`
  (histogram), `email_permanent_failures_total{template=...}`.
- Log every send attempt with `emailId`, `template`, `recipient` (hashed in
  logs), `attempt`, `outcome`. Never log the payload (may contain
  semi-sensitive info like reservation details).
- Alert (operational): `email_permanent_failures_total` rising or
  `email_outbox_size{status=PENDING}` growing unbounded.

## Tests Required

- Unit: outbox enqueue, retry-with-backoff state machine, template
  rendering.
- Integration with Testcontainers Postgres: worker picks pending rows,
  marks sent, retries on failure with backoff timing.
- Integration with GreenMail (or fake SMTP server) for end-to-end SMTP path.
- Architectural: `notification` module exposes only its port and listeners;
  no other module directly accesses `outbox_emails`.

## Acceptance Criteria

- A `CustomerRegistered` event in dev triggers a real email to the
  registered user via Gmail SMTP.
- Sending to an invalid SMTP host retries with backoff and eventually marks
  `FAILED_PERMANENT`.
- `MAIL_TO_OVERRIDE` in dev redirects every outbound mail to one address.

## Open Questions

- Should templates live in the repository or be admin-editable via UI?
  Default: repository (Thymeleaf files under
  `src/main/resources/templates/email/`). UI editing is future.
- User language preference column: add to `users` in this spec or in a
  later one? Proposal: add `preferred_locale` to `users` here (low cost,
  unlocks i18n correctness for all emails).

## Out of Scope

- SMS and push notifications.
- Marketing/bulk email.
- Open/click tracking.
- Provider-specific abstractions beyond the SMTP common denominator.
- Template UI editor.
