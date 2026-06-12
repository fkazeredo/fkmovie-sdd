# 0012 - Pricing

Status: Implemented
Related ADRs: 0001

> Implementation notes (backend):
> - Open Questions resolved: VIP seed surcharge = R$10,00 (1000 cents); rounding is to the cent
>   (round-half-up; `half = ceil(full/2)`), not to "pretty" R$0,50 multiples.
> - Public facade `PriceCalculator.quote(ctx, type, ticketType)` is pure over an in-memory config
>   snapshot (`PricingConfig`), so the hot seat-map path never hits the DB per seat. The snapshot
>   loads after Flyway and reloads after an admin change commits (`PricingConfigChanged`
>   AFTER_COMMIT listener).
> - Surcharge (≥0; ACCESSIBLE/COMPANION must be 0) and multiplier range [0.10, 2.00] are validated
>   in the service (not bean validation) so they surface `pricing.invalid-surcharge` /
>   `pricing.invalid-multiplier` rather than a generic `validation.error`. DB CHECKs are the backstop.
> - Admin endpoints take a bulk PUT (list of entries). `day_of_week` is stored as `INT` (ISO
>   Mon=1..Sun=7) for a clean JPA mapping; the weekday is the screening's start day in
>   America/Sao_Paulo.
> - Harmonizes the SPEC-0011 `SeatPricing` seam: the flat mock was removed and the booking seat map
>   now prices via `PriceCalculator` (VIP seats show base + surcharge).
> - `HalfPriceCategory` + `documentReference` and the price snapshot at reservation are deferred to
>   SPEC-0014 (the quote does not need the category; half is always 50%).
> - Module graph stays acyclic: `booking → pricing → cinema` (only the `SeatType` enum).

## Goal

Compute the price of a seat for a screening from: the screening's base
price, the seat type, the day of week of the session, and the ticket type
(full / half — meia-entrada, Lei 12.933/2013).

## Scope

`pricing` module: configuration tables, a pure calculation service exposed
as the module's public API, admin endpoints to manage modifiers. Price
snapshotting at reservation time belongs to 0014.

## Business Context

Brazilian cinema pricing: a base price per session, premium seats cost
more, mid-week promos are common, and half-price (50%) is a legal right for
students, elderly (60+), PCD (+companion when required), and low-income
youth (ID Jovem). Eligibility is declared by the customer and verified
physically at the cinema entrance — the system stores the declaration.

## Business Rules

- Price formula (integer cents, BRL):

```txt
full = round_half_up( (base + seatTypeSurcharge) * dayOfWeekMultiplier )
half = ceil(full / 2)
```

- `seatTypeSurcharge` per type, admin-configurable. Seed: STANDARD 0,
  VIP +1000, ACCESSIBLE 0, COMPANION 0. ACCESSIBLE and COMPANION MUST NOT
  have positive surcharge (legal sensitivity — enforced by validation).
- `dayOfWeekMultiplier` per weekday, admin-configurable, seed 1.00 for all
  days. Multiplier range allowed: 0.10–2.00.
- Day of week is the **screening's start day in America/Sao_Paulo** (a
  Wednesday promo applies to Wednesday sessions, not Wednesday purchases).
- Half price is exactly 50% rounded up to the cent (Lei 12.933 — 50% do
  valor cobrado). Half-price categories: `STUDENT, ELDERLY, PCD,
  PCD_COMPANION, LOW_INCOME_YOUTH`.
- A half-price selection MUST carry `halfPriceCategory` and
  `documentReference` (free text ≤ 60 chars, e.g., carteirinha number) —
  stored for audit per the law's verification model. The system does NOT
  validate eligibility online.
- Price computation is deterministic and side-effect free; the same inputs
  produce the same output (pure function over config snapshot).
- Changing modifiers affects only FUTURE price computations; reservations
  hold snapshots (0014).

## Input/Output Examples

Base 3000, VIP (+1000), Wednesday 0.70 → full = round(4000×0.70) = 2800;
half = 1400.

## API Contracts

Internal public API (module facade):

```java
PriceQuote quote(ScreeningPricingContext ctx, SeatType type, TicketType ticketType);
```

Admin REST:

- `GET/PUT /api/admin/pricing/seat-types` — list/update surcharges.
- `GET/PUT /api/admin/pricing/weekdays` — list/update multipliers.

## Events

- `PricingConfigChanged(changedByAdminId, occurredAt)` — audit.

## Persistence Changes

- `pricing_seat_type(seat_type PK, surcharge_cents INT NOT NULL)`.
- `pricing_weekday(day_of_week PK SMALLINT 1-7, multiplier NUMERIC(3,2))`.
- Seeds as above.

## Validation Rules

- Surcharge ≥ 0; ACCESSIBLE/COMPANION surcharge must be 0.
- Multiplier within [0.10, 2.00].
- Admin-only endpoints.

## Error Behavior

| HTTP | Error code | When |
|------|------------|------|
| 400 | `pricing.invalid-surcharge` | Negative or accessible>0. |
| 400 | `pricing.invalid-multiplier` | Out of range. |

## Observability Requirements

- Audit log on config change with before/after values.

## Tests Required

- Unit: formula incl. rounding edges (odd cents → half rounds up),
  timezone day boundary (session 00:30 Sat in SP = Saturday even though
  Friday in UTC).
- Integration: admin updates apply to next quote; ACCESSIBLE surcharge
  rejection.

## Acceptance Criteria

- Quotes match the formula across seat types, weekdays and ticket types.

## Open Questions

- VIP seed surcharge R$10,00 — owner can adjust via admin UI; confirm seed.
- Rounding to "pretty" prices (e.g., nearest R$0,50) — v1 rounds to cent.
  Confirm acceptable.

## Out of Scope

- Promo codes, combos, loyalty. Dynamic pricing. Snapshotting (0014).
