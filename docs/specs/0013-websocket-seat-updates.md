# 0013 - WebSocket Seat Updates

Status: Implemented
Related ADRs: 0009, 0005, 0002

> Implementation notes (backend):
> - Open Question resolved: anonymous viewers get **no** realtime in v1 — CONNECT requires a JWT, so
>   pre-login uses REST polling. `/ws` is plain WebSocket (no SockJS).
> - Transport in `com.fksoft.infra.socket` (`WebSocketConfig` + `StompAuthChannelInterceptor`):
>   in-memory broker (`/topic`, `/queue`, `/app`, `/user`); the CONNECT frame is authenticated with
>   the existing `JwtDecoder` (the STOMP JWT-on-CONNECT promised in 0003 lands here) and the principal
>   is the userId. The `/ws` handshake is permitAll (auth is on CONNECT, not the HTTP upgrade).
> - Publishers in `com.fksoft.application.realtime` (ADR 0012; originally ADR 0009): consume the
>   booking events AFTER_COMMIT and send via `SimpMessagingTemplate` — read-only, never changing
>   seat state. Seat row/number resolved via the cinema facade.
> - Metrics: `ws_connections_active`, `ws_connect_rejected_total`, `ws_messages_sent_total{type}`.
> - To make the payment/confirmation e2e tests deterministic, the mock payment dispatcher is driven
>   explicitly in tests (long poll interval); no behavior change in prod.

> Out of scope still: external broker / multi-instance relay (ADR 0002 revision); frontend
> subscription (0023/0024).

## Goal

Clients viewing a seat map receive seat status changes in real time.
Realtime is a core product requirement.

> Note: the STOMP CONNECT JWT validation from spec 0003 lands here (no transport
> existed at 0003). Reuse the `JwtDecoder` bean from `com.fksoft.infra.security`
> in a CONNECT `ChannelInterceptor` to authenticate and bind the principal.

## Scope

Two parts (ADR 0009, refined by ADR 0012): STOMP transport configuration in
`com.fksoft.infra.socket`, and the seat-update publisher in the delivery layer at
`com.fksoft.application.realtime`. Also the per-user reservation
status channel used by 0015/0016.

## Business Rules

- WebSocket endpoint: `/ws`. Simple in-memory broker (`/topic`, `/queue`),
  app prefix `/app`, user prefix `/user` (ADR 0002: single instance).
- CONNECT MUST carry `Authorization: Bearer <jwt>` STOMP header; invalid or
  missing token → connection rejected. (Viewing the seat map via REST is
  public, but the live channel requires auth — it is only needed by users
  in the reservation funnel, who are authenticated.)
- Seat updates topic: `/topic/screenings/{screeningId}/seats`. Message:

```json
{ "type": "SEAT_STATUS_CHANGED", "screeningId": "uuid",
  "seats": [ { "seatId": "uuid", "row": "A", "number": 1,
               "status": "HELD" } ] }
```

- Reservation status (private): user destination
  `/user/queue/reservations`. Message:

```json
{ "type": "RESERVATION_STATUS_CHANGED", "reservationId": "uuid",
  "status": "CONFIRMED" }
```

  Sent only to the reservation owner's principal — UUID-guessing exposure
  of a shared topic is avoided.
- Publishing MUST happen only after transaction commit:
  booking services publish internal domain events; a
  `@TransactionalEventListener(phase = AFTER_COMMIT)` sends the STOMP
  message. Realtime code MUST NOT own booking rules nor change seat state.
- No top-level `realtime` business module (ADR 0009).
- Message contracts are stable; evolutions add fields, never repurpose.

## API Contracts

WebSocket/STOMP contracts above. No REST.

## Events

Consumes internal events (produced by 0014/0016/0017/0018):

- `SeatsStatusChanged(screeningId, List<SeatStatus>)` → topic message.
- `ReservationStatusChanged(reservationId, userId, status)` → user queue.

## Persistence Changes

None.

## Validation Rules

- Channel interceptor validates JWT on CONNECT and binds the principal
  (userId) for `/user` destinations.

## Error Behavior

- Invalid token on CONNECT → STOMP ERROR frame, connection closed.
- Subscription to other users' queues is impossible by design (user
  destinations are principal-bound by Spring).

## Observability Requirements

- Metrics: `ws_connections_active` (gauge), `ws_messages_sent_total{type}`,
  `ws_connect_rejected_total`.
- Log CONNECT/DISCONNECT with userId; never log the token.

## Tests Required

- Integration: STOMP CONNECT with valid JWT succeeds; invalid rejected.
- Integration: after-commit ordering — listener fires only on commit;
  rollback sends nothing (test with forced rollback).
- Integration: seat-update message lands on the right topic with the right
  payload; reservation update lands only on the owner's queue.

## Acceptance Criteria

- Two browser sessions on the same seat map see each other's holds within
  1s in local dev.

## Open Questions

- Should anonymous seat-map viewers get realtime too (auth-free SUBSCRIBE
  on the public topic)? v1 decision: no — REST polling on refresh is enough
  pre-login; revisit if product wants live maps for anonymous browsing.

## Out of Scope

- Reservation logic (0014+). Frontend subscription (0023). External broker,
  multi-instance relay (ADR 0002 revision).
