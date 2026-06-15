# ADR 0011: Domain exceptions free of transport concerns

## Status

Accepted (owner decision; refines ADR 0010)

## Context

Business exceptions extended `BusinessException`, which carried an **`HttpStatus`**,
`httpHeaders()` and a `fields()` method returning `ApiErrorResponse.FieldViolation`
(the API response DTO). HTTP and the response shape — pure presentation concerns —
had leaked into the domain layer; `SeatsUnavailableException` even imported the API
response type. The owner wants the domain exceptions **pure** (only domain data) and
the translation to HTTP done in the presentation layer.

Constraint (ADR 0010): controllers live in `com.fksoft.application.<mod>.api`, and the
ArchUnit rule forbids `application → infra`. So types the controllers import cannot
live in `infra`.

## Decision

- **`DomainException`** (kernel `shared.error`) replaces `BusinessException`: it carries
  only a stable `code` (== i18n message key) and optional message args. No `HttpStatus`,
  no headers, no response DTO. Extra domain data is exposed via two kernel interfaces,
  `ErrorDetails` (key/value pairs, e.g. unavailable seat ids) and `RateLimited` (a
  `Duration` to wait) — domain data, not transport classification.
- The **presentation layer** (`com.fksoft.infra.web`) owns the HTTP translation, chosen
  by the owner as a **registry by exception type**: `HttpErrorMapping`
  (`Map<Class<? extends DomainException>, HttpStatus>`) + the `@RestControllerAdvice`
  `GlobalExceptionHandler`, which resolves the i18n message, maps `ErrorDetails` →
  response `fields` and `RateLimited` → `Retry-After`. `ApiErrorResponse` moved here too.
- A build-time test (`HttpErrorMappingCompletenessTest`) fails if any `DomainException`
  subclass lacks a status, so the registry's only weakness — a forgotten entry silently
  defaulting to 422 — cannot happen unnoticed.
- `PageResponse` stays in the kernel (`shared.pagination`) because controllers import it
  (it can't be `infra` under the `application ↛ infra` rule), but now **only controllers
  produce it** — the two services that returned it now return Spring `Page<View>`.
- The current-user accessor becomes a port: `UserContextProvider` is now an interface in
  `shared.security`, and the `SecurityContextHolder` adapter
  (`SecurityContextUserProvider`) lives in `infra.security`. `UserContext` dropped its
  unused `tenantId`.

## Consequences

Positive: the domain layer is free of HTTP/transport types; "what status is this error?"
is answered in one presentation file; adding a real provider or changing a status never
touches the domain. The completeness test keeps the registry honest.

Negative: `HttpErrorMapping` lists all ~43 exceptions and imports them (infra → domain,
allowed) — the explicit cost of the registry approach the owner chose. A few exception
types are referenced by the presentation; that coupling is one-directional
(infra → domain) and test-guarded.

## Alternatives Considered

- **`DomainException` + semantic `ErrorKind` enum** (domain declares NOT_FOUND/CONFLICT/…;
  handler maps kind → status). Rejected by the owner in favor of zero classification in the
  domain (pure exceptions + presentation registry).
- **Semantic base classes per kind** (`NotFoundException`, `ConflictException`, …).
  Rejected for the same reason.
- **Keep `HttpStatus` in the exception (status quo).** Rejected: that is exactly the
  transport leak being removed.
