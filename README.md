# fkmovies - A Spec-Driven Development Proof of Concept

> **What this repository is.** `fkmovies` is a fully working cinema-ticketing system
> (backend + frontend + observability), but the *product* is not the point. This is a
> **proof of concept** I ran to test one question:
>
> **Can spec-driven development, backed by deterministic guardrails, make AI-assisted
> software engineering produce production-grade, maintainable code - instead of a pile of
> plausible-looking snippets?**
>
> The whole system was built by an AI coding agent (Claude Code) working against
> human-authored **specs**, **ADRs**, and an **architecture guideline**, with the rules that
> a machine *can* check encoded as failing tests rather than prose. This README documents
> the experiment, the process, and the numbers.

---

## TL;DR - the experiment in numbers

Built in **5 days** (2026-06-11 -> 2026-06-15), single author, **60 commits**.

| Dimension | Count |
|---|---:|
| Feature specs (`SPEC-0000` -> `SPEC-0026`) | **27** |
| Architecture Decision Records | **12** |
| On-demand architecture guides | **12** |
| Database migrations (Flyway) | **17** |
| Business modules (Spring Modulith) | **7** + 1 error kernel |
| REST controllers | **16** |
| Automated tests (60 classes) | **210** - **0 failures, 0 skipped** |
| Architecture rules enforced (ArchUnit) | **14** + Modulith boundary verification |

| Lines of code | Count |
|---|---:|
| Backend production (Java) | **10,915** |
| Backend tests (Java) | **5,582** |
| Frontend (TypeScript) | **4,449** |
| Specs + ADRs + architecture docs (Markdown) | **5,788** |

Two ratios that capture the philosophy: **test code ~51% of production code**, and there is
**roughly one line of spec/decision/architecture prose for every two lines of production
code**. The design was written down *before* and *around* the code, not reverse-engineered
from it.

Commit breakdown: `28 feat / 21 docs / 5 fix / 3 refactor / 1 perf / 1 chore`.

---

## Project size & how long this would take *without* an AI agent

By size this is a small-to-mid **production** system, not a toy:

- **~21,000 lines** of application code (Java backend + tests + Angular frontend),
- **~5,800 lines** of specs, ADRs and architecture documentation,
- **27 features**, **7 modules**, **17 database migrations**, **210 automated tests**, plus
  real production concerns: authentication, payments, realtime, i18n and a full observability
  stack.

So how long would the **same scope at the same quality bar** take to build *conventionally*,
without Claude Code? Using common software-productivity figures - a sustained net output of
tested, reviewed production code, and treating the ~5,800 lines of design docs as real work in
their own right - an order-of-magnitude estimate:

| Scenario (no AI agent) | Estimated effort for equivalent scope |
|---|---|
| One experienced full-stack engineer | **~4-8 months** of full-time work |
| A small team of 2-3 engineers | **~6-12 weeks** of calendar time |

This proof of concept assembled it in **5 calendar days** of human-directed agent work -
roughly a **10-30x compression in calendar time** for equivalent output, with the human acting
as architect and reviewer rather than implementer.

> **Caveat - read this honestly.** The figures above are an *estimate*, not a controlled study;
> software effort varies widely with quality bar, experience and scope creep. And the 5 days
> were **not** effort-free: they required real human work - authoring the specs and ADRs,
> making the architectural decisions, and reviewing every change. The acceleration is in
> *implementation throughput*, not in skipping the thinking. That is precisely the point of the
> experiment: the human spends their time on **intent and judgment**, the agent on **execution**,
> and the guardrails keep the result honest.

---

## The hypothesis

AI agents are good at producing code that *looks* right. They are bad, by default, at:

- keeping architectural boundaries over many changes,
- not silently inventing business rules,
- writing the unglamorous parts (migrations, i18n, error contracts, tests),
- and resisting entropy as the codebase grows.

The bet of this PoC: **if the intent is captured as living specs, the cross-cutting decisions
as ADRs, and the invariants as executable checks, then an agent can be held to a real
engineering standard** - and a human stays in the loop as the *owner of decisions*, not the
typist.

So the control system has three layers (see [CLAUDE.md](CLAUDE.md)):

1. **Always-loaded invariants** - [`CLAUDE.md`](CLAUDE.md) (~90 lines): rules valid for 100% of
   tasks (avoid overengineering, never invent business rules, spec-driven, tooling is
   authoritative, no loose ends) plus a *Routing Map*.
2. **On-demand knowledge** - [`architecture/`](architecture/): detailed guidance loaded only
   when a task touches that area (backend, persistence, security, messaging, frontend,
   testing, delivery...). Keeps the context small and the signal high.
3. **Deterministic enforcement** - rules a tool can check are *code, not prose*:
   - [`ArchitectureTest.java`](backend/src/test/java/com/fksoft/architecture/ArchitectureTest.java)
     (ArchUnit) and [`ModularityTests.java`](backend/src/test/java/com/fksoft/architecture/ModularityTests.java)
     (Spring Modulith) fail the build on boundary violations, `@Data` entities, `*Impl`
     naming, field injection, controller->repository access, and cross-module persistence leaks.
   - Spotless + Checkstyle (formatting and style as a build fact).
   - `.claude/settings.json` denies destructive commands and protects secrets.

---

## How the process actually went

The work followed a strict loop, one feature at a time:

```
write / update SPEC  ->  decide cross-cutting concerns as ADR  ->  encode invariants as tests
        ->  implement  ->  ./mvnw verify (ArchUnit + Modulith + tests + format)  ->  commit
```

A few representative moments that show the method working:

- **Specs came first, code second.** All 27 specs ([`docs/specs/`](docs/specs/)) were authored
  as living contracts - scope, endpoints, status codes, error codes, validation, open
  questions. When a requirement was undecided, it was *mocked and deferred to a harmonizing
  spec* rather than guessed (e.g. the seat read-model before reservations existed).
- **Decisions were recorded, not implied.** 12 ADRs ([`docs/adr/`](docs/adr/)) capture the
  *why*: modular monolith (0001), JWT auth (0005), mock payment via async webhook (0006),
  realtime scoping (0009), a centralized infrastructure layer (0010), transport-free domain
  exceptions (0011), and a three-layer package architecture (0012).
- **The architecture was refactored under test, late in the project.** The last three commits
  reshaped the entire backend into `domain` / `application` / `infra` layers (ADR 0012) -
  **~320 files moved**, business exceptions made transport-free, the delivery layer made
  entity-free. Because the boundaries were executable (ArchUnit + Modulith) and the behavior
  was pinned by 210 tests, a refactor of that size landed **green, behavior-preserving, in one
  pass**. That is the whole thesis in one event: *guardrails make large change safe.*
- **Honest failure handling.** When the full test battery "failed" with 119 errors, the cause
  was a wedged local Docker daemon (Testcontainers couldn't find an engine), not the code -
  diagnosed, Docker restarted, and the suite then passed **210/0/0**. The guardrails told the
  truth instead of hiding it.

---

## What got built (the product)

A cinema ticketing system covering the real workflow end to end:

- **Accounts & auth** - customer registration with email verification, JWT login with
  single-use refresh-token rotation and theft detection, password reset, admin/operator user
  management, login rate limiting.
- **Catalog & scheduling** - rooms and typed seats, movies CRUD, screenings with
  overlap/lead-time rules, a public screenings listing.
- **Booking** - a per-screening seat read-model, **temporary seat holds**, purchase
  confirmation, expiration, customer cancellation with refunds, "my reservations", operator
  ticket lookup/reprint.
- **Pricing** - seat-type surcharges and weekday multipliers with a live-reloaded snapshot.
- **Payments** - a mock gateway with an asynchronous, signed webhook (exactly-once settlement).
- **Realtime** - WebSocket/STOMP seat-status and reservation-status updates, published only
  after commit.
- **Frontend** - an Angular app for the customer journey and the admin/operator console.
- **Observability** - structured JSON logs -> Loki, metrics -> Prometheus, dashboards in
  Grafana, correlation IDs end to end.

End-user walkthrough (PT-BR): [`docs/manual-do-usuario.md`](docs/manual-do-usuario.md).
Product overview: [`docs/project.md`](docs/project.md).

---

## Architecture at a glance

Three layers under `com.fksoft` (ADR 0012); the **domain is the only pure layer**:

```
com.fksoft.domain.<module>     hexagon core: services, entities, repositories, events,
                               value/view records, business exceptions, module facades (ports)
                               + domain.error  (DomainException, ErrorDetails, RateLimited)
com.fksoft.application          delivery (driving adapters): api + api.dto, realtime + realtime.dto
com.fksoft.infra.<concern>      driven adapters: security, web, email, integration, i18n,
                               time, socket, observability
```

Dependency rule, enforced by ArchUnit: **`domain` must not depend on `application` or `infra`**;
both may depend on `domain`; `application` may depend on `infra`. Business modules never reach
into each other's persistence - they collaborate through public facades and domain events
(verified by Spring Modulith).

**Stack:** Java 21 / Spring Boot 4 / Spring Modulith / PostgreSQL 16 + Flyway / JWT /
WebSocket/STOMP / Testcontainers / ArchUnit / Angular (standalone + signals) / Docker Compose /
Prometheus / Grafana / Loki / Alloy.

---

## Documentation map

| If you want to understand... | Read |
|---|---|
| The operating rules the agent follows every session | [`CLAUDE.md`](CLAUDE.md) |
| The architecture principles & per-area guidance | [`architecture/`](architecture/) |
| What each feature must do (living contracts) | [`docs/specs/`](docs/specs/) |
| Why a cross-cutting decision was made | [`docs/adr/`](docs/adr/) |
| How a user actually uses the app (PT-BR) | [`docs/manual-do-usuario.md`](docs/manual-do-usuario.md) |
| The product at a glance | [`docs/project.md`](docs/project.md) |
| The executable architecture rules | [`ArchitectureTest.java`](backend/src/test/java/com/fksoft/architecture/ArchitectureTest.java) / [`ModularityTests.java`](backend/src/test/java/com/fksoft/architecture/ModularityTests.java) |

---

## Run it

Prerequisites: **Docker** (running) and **Java 21** for backend dev; **Node 22+** for frontend dev.

### Full stack (one command)

Secrets come from `.env.local` (git-ignored), injected into the backend via `env_file` - they
never appear on the command line.

```bash
cp .env.example .env.local      # then set a real JWT_SECRET (>=32 chars), mail + webhook settings
docker compose up -d --build
```

Open:

- App API - <http://localhost:8080> (readiness: `/actuator/health/readiness`)
- Frontend - <http://localhost:4200>
- Grafana (dashboards + logs) - <http://localhost:3000> (admin / admin)
- Prometheus - <http://localhost:9090>

Stop with `docker compose down` (keeps the named volumes; never use `-v` unless wiping data).
The dev DB is published on `5434` (`DB_PORT`) since `5432` is often taken; inside the Compose
network the backend reaches it as `db:5432`.

### Backend only

```bash
cd backend
./mvnw spotless:apply   # format
./mvnw verify           # tests + ArchUnit + Modulith + spotless:check + checkstyle (needs Docker)
```

### Frontend only

```bash
cd frontend
npm install
npm start               # http://localhost:4200, /api and /ws proxied to :8080
npm run lint && npm test && npm run build
```

---

## Reusing the guideline in your own project

The control system (specs + ADRs + always-loaded rules + executable checks) is the reusable
part. To adopt it:

1. Copy [`CLAUDE.md`](CLAUDE.md), [`architecture/`](architecture/), [`docs/`](docs/) and
   `.claude/` to your repository root.
2. Copy the two architecture tests into your backend, fixing the root package.
3. Merge `backend/config/pom-snippets.xml` into your `pom.xml`; copy `checkstyle.xml` to
   `config/`.
4. Adjust the *Project commands* section of `CLAUDE.md` and the package names in the
   architecture tests.

Maintenance principle: **prefer adding an ArchUnit rule over adding prose**, and keep
`CLAUDE.md` short - when it grows past ~120 lines, move content into `architecture/` and route
to it.

---

## Takeaways from the experiment

- **Spec-first + ADRs gave the agent a stable target.** Most "AI hallucination" risk is really
  *underspecification*; writing the contract first removed the guesswork.
- **Executable invariants beat documentation.** Boundaries that fail the build (ArchUnit /
  Modulith) survived a 320-file refactor; prose alone would not have.
- **Tests are the license to refactor.** 210 behavior tests turned a risky restructuring into a
  routine one.
- **The human stays the decision owner.** The agent surfaced conflicts and asked instead of
  inventing - exactly when business rules, contracts, or architecture were at stake.

*Built as a learning exercise in spec-driven development with an AI coding agent.*
