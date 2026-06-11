# 0001 - Backend Foundation

Status: Draft
Related ADRs: 0001, 0002, 0008 (frontend stack referenced for proxy alignment)

## Goal

Create the Spring Boot backend skeleton that all subsequent backend specs
build on. The application boots, exposes a health check, validates database
connectivity through Flyway, and passes the architectural tests with an
empty module set.

## Scope

Project structure, dependencies, configuration, local infrastructure
(Docker Compose), and the architectural enforcement layer. No business
endpoints, no entities, no controllers beyond the health check.

## Business Context

This is the foundation for the modular monolith described in ADR 0001. The
package root is `com.fksoft`. Modules under `com.fksoft.application` are
introduced from spec 0007 onward.

## Business Rules

- The application MUST boot with `mvn spring-boot:run` against a Postgres
  instance from `compose.yaml`.
- Backend configuration MUST read database URL, username, password and JWT
  secret from environment variables. No secrets in committed config.
- `.env.local` MUST be git-ignored; `.env.example` MUST list every variable.
- JPA `open-in-view` MUST be `false`.
- Hibernate `ddl-auto` MUST be `validate`. Schema changes go through Flyway.
- The actuator `/actuator/health` endpoint MUST be available; liveness and
  readiness MUST be distinguished.
- `ArchUnit` and `Spring Modulith` tests MUST be present and pass even with
  no modules implemented yet.
- Time handling MUST default to UTC at the JVM level (`-Duser.timezone=UTC`
  via Dockerfile/JVM flag).

## Technical Stack

- Java 21 (LTS).
- Spring Boot 4.x.
- Maven (single-project, no multi-module per `architecture/backend.md`).
- Postgres 16+.
- Flyway.
- Spring Web MVC.
- Spring Data JPA.
- Spring Validation (Jakarta).
- Spring WebSocket / STOMP.
- Spring Security 6.x (configured for JWT in spec 0003).
- Spring Actuator.
- Testcontainers (Postgres module).
- JUnit 5, AssertJ.
- ArchUnit 1.3+.
- Spring Modulith starter test.
- Spotless + Checkstyle (already from template).

## API Contracts

`GET /actuator/health/liveness` → `200 {"status":"UP"}`
`GET /actuator/health/readiness` → `200 {"status":"UP"}` when database is
reachable; `503` otherwise.

## Events

Not applicable.

## Persistence Changes

- `compose.yaml`: Postgres service `db` (port 5432), persistent volume,
  healthcheck.
- Flyway baseline migration `V1__baseline.sql` empty (placeholder for first
  real migration in 0007).

## Validation Rules

- Typed configuration via `@ConfigurationProperties` with `@Validated` for
  critical settings (datasource, JWT, mail, payment).
- Startup MUST fail fast if a required env var is missing (no silent default
  for security-sensitive values).

## Error Behavior

- Global `@RestControllerAdvice` handler in `com.fksoft.shared.error`
  returns the standard error format:

```json
{ "code": "...", "message": "...", "fields": [] }
```

- Default `code`: `internal.error` for unhandled exceptions.
- Logging: structured JSON via `logstash-logback-encoder`. Correlation ID
  via MDC filter on every HTTP request.

## Observability Requirements

- Logs: JSON to stdout. Fields: `timestamp`, `level`, `logger`, `message`,
  `correlationId`, `userId` (when authenticated), `tenantId`. Never log
  secrets, tokens, passwords or full request bodies for auth endpoints.
- Metrics: Prometheus via Spring Boot Actuator + Micrometer at
  `/actuator/prometheus`.
- Health checks split into liveness (process up) and readiness (DB up).

## Tests Required

- Integration: application context loads with Testcontainers Postgres.
- Integration: `/actuator/health/liveness` and `/actuator/health/readiness`
  return 200 when DB is up.
- Architectural: `ArchitectureTest` and `ModularityTests` pass on the empty
  module set.

## Acceptance Criteria

- `mvn verify` passes locally with `compose.yaml` Postgres running.
- `docker compose up -d db && mvn spring-boot:run` boots the app on port
  8080.
- `curl http://localhost:8080/actuator/health/readiness` returns `UP`.
- All architectural tests green.
- `.env.example` lists: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`,
  `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL`, `MAIL_HOST`, `MAIL_PORT`,
  `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_TO_OVERRIDE`,
  `PAYMENT_WEBHOOK_SECRET`, `APP_TIMEZONE=UTC`.

## Open Questions

- Postgres major version pin: 16 or 17? (Default 16 unless infra requires
  otherwise.) Decision needed before staging.

## Out of Scope

- Business entities, controllers, services beyond health.
- Seat map, reservation, payment.
- WebSocket business publishing (transport config will land with spec 0013).
- Frontend.
- CI pipeline (separate later).
