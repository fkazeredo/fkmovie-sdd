# 0015 - Mock Payment Gateway and Webhook

Status: Implemented
Related ADRs: 0006, 0007

> Implementation notes (backend):
> - Open Question resolved: webhook path uses a provider suffix `/api/webhooks/payments/mock`; a
>   real adapter adds its own path. Public endpoint (no JWT), HMAC-verified.
> - The mock signs with `WebhookJson` (its own ObjectMapper) and delivers over HTTP to the app's own
>   endpoint, resolved from the live `local.server.port` (prod: configured base URL). The @Scheduled
>   dispatcher is a separate bean from the transactional worker so the claim/deliver transactions
>   apply (the outbox pattern).
> - Idempotency via `payment_webhook_events` UNIQUE(payment_id, event_type); duplicate → 200 no-op.
>   HMAC verified before parsing (401 `payment.invalid-signature`); malformed → 422
>   `payment.invalid-payload`.
> - `payments.reservation_id` is a soft reference (no FK) to keep the ledger decoupled from booking
>   (ADR 0006 boundary). The module publishes events and never touches reservations.
> - Refund path (`requestRefund`) is in place; consumed by cancellation (0018) and late-success (0016).

## Goal

Deliver the `payment` module: the `PaymentGateway` port, the async mock
adapter that simulates a real provider (delayed signed webhook), the webhook
endpoint, and the `payments` ledger. Spec 0016 consumes this to confirm
reservations.

## Scope

Payment request creation, mock delivery job, webhook reception with HMAC
validation and idempotency, refund request (consumed by 0018). No real
provider integration.

## Business Rules

- Port (module public API):

```java
PaymentRequestResult request(PaymentRequest req);   // → paymentId, PENDING
RefundRequestResult requestRefund(RefundRequest req);
```

- `MockPaymentGateway` persists a `mock_payment_jobs` row scheduled for
  `now + delay` (default 3 s, overridable per request metadata for tests);
  a scheduled worker then POSTs a webhook to the application's own endpoint
  `POST /api/webhooks/payments/mock`.
- Mock outcome: `SUCCEEDED` by default. Deterministic failure hooks for
  tests/staging: request metadata `forceOutcome=FAILED`, or amount ending
  in `…13` cents fails (documented, dev/staging only — disabled by config
  in prod profile).
- Webhook MUST be signed: header `X-Signature: HMAC-SHA256(body,
  PAYMENT_WEBHOOK_SECRET)`. Invalid/missing signature → 401, body not
  processed.
- Webhook processing MUST be idempotent, keyed on `(payment_id,
  event_type)`: duplicates return 200 without side effects.
- Webhook handler updates the `payments` row and publishes
  `PaymentSucceeded | PaymentFailed | RefundSucceeded` internal events.
  Booking (0016/0018) reacts; the payment module does NOT touch
  reservations directly (module boundary).
- A success webhook for a reservation no longer `AWAITING_PAYMENT`
  (e.g., cancelled by timeout 0017) MUST trigger an automatic refund
  request and log a WARN business event (`payment.late-success-refunded`).
  This rule lives in booking's listener (0016) — stated here for the
  contract.
- Amounts are integer cents BRL and MUST match the reservation total —
  validated by booking when requesting.

## Input/Output Examples

Webhook body:

```json
{ "eventId": "uuid", "eventType": "PAYMENT_SUCCEEDED",
  "paymentId": "uuid", "reservationId": "uuid", "amountCents": 4500,
  "occurredAt": "2026-06-20T19:02:03Z" }
```

## API Contracts

- `POST /api/webhooks/payments/mock` — public path, HMAC-protected, no JWT
  (gateways don't hold user tokens). Always 200 on accepted/duplicate,
  401 on bad signature, 422 on malformed body.

## Events

Publishes internal: `PaymentSucceeded(paymentId, reservationId,
amountCents)`, `PaymentFailed(...)`, `RefundSucceeded(...)`,
`RefundFailed(...)`.

## Persistence Changes

- `payments(id UUID PK, tenant_id, reservation_id FK, kind CHARGE|REFUND,
   status PENDING|SUCCEEDED|FAILED, amount_cents, provider 'MOCK',
   provider_payment_id, created_at, settled_at)`.
- `payment_webhook_events(id PK, payment_id, event_type, received_at,
   UNIQUE(payment_id, event_type))` — idempotency ledger.
- `mock_payment_jobs(id, payment_id, deliver_at, outcome, delivered_at)`;
  index `(delivered_at NULLS FIRST, deliver_at)`.

## Validation Rules

- HMAC before parsing trust; payload schema validation; amount > 0;
  kind/status enums by CHECK.

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 401 | `payment.invalid-signature` | HMAC mismatch. |
| 422 | `payment.invalid-payload` | Schema violation. |

Internal classification for gateway calls follows
`messaging-and-integrations.md` failure taxonomy.

## Observability Requirements

- Metrics: `payments_total{kind,status}`, `payment_webhooks_total{outcome}`,
  `payment_webhook_signature_failures_total`, mock delivery latency.
- Log webhook receipt with `paymentId`, `eventType`, idempotency outcome.
  Never log the secret or full signature.

## Tests Required

- Unit: HMAC validation, idempotency key behavior.
- Integration: request → mock job → webhook → `PaymentSucceeded` event
  published (Testcontainers, awaitility).
- Integration: duplicate webhook = no second event; bad signature = 401 and
  no processing; forced failure path.

## Acceptance Criteria

- End-to-end in dev: a payment request settles via webhook within ~3 s and
  the internal event fires exactly once.

## Open Questions

- Webhook path uses `/mock` suffix; the future real adapter adds its own
  path (e.g., `/stripe`). Confirm pattern.

## Out of Scope

- Real provider adapters. Reservation transitions (0016/0018). Payment
  methods UI (mock needs none).
