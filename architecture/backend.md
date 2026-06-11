# Backend Architecture (Java / Spring Boot)

> Read when: writing or changing any backend code — services, entities, DTOs, mapping,
> validation, error handling, dates, naming, comments.

## Default style and package layout

Pragmatic modular hexagonal architecture, organized by business domain. Hexagonal is a
principle, not folder theater. Spring, Lombok, Bean Validation and JPA annotations are
acceptable; the project does not pretend Spring does not exist.

```txt
com.company.project
  application
    order            <- module root = domain/application core
      Order.java  OrderStatus.java  OrderService.java  OrderRepository.java
      OrderCancelledEvent.java  OrderCannotBeCancelledException.java  OrderAccessPolicy.java
      api/    OrderController.java  CreateOrderRequest.java  OrderResponse.java
      queue/  OrderCancelledConsumer.java
      infra/  OrderCsvImporter.java
    customer ...
  infra      persistence/ messaging/ security/ external/ config/
  shared     error/ i18n/ observability/ pagination/ validation/
```

**MUST NOT** create `domain/application/ports/adapters/in/out` folder trees unless complexity
truly justifies it. Single Maven project with strong package modularity; multi-module only
for shared libraries, separate deployables, very large codebases or independent ownership.

## Services and use cases

A module service is an Application Service: coordinates flow, transactions, repositories,
domain behavior and results. It **MUST NOT** become a dumping ground for business rules —
domain rules live in entities, value objects, enums with behavior, policies or domain
services. **MUST NOT** create explicit `UseCase` classes by default.

## Domain entities and JPA

JPA entities **MAY** be domain entities; no artificial domain/persistence separation by
default. Anemic models are not acceptable: entities **MUST** protect invariants and expose
meaningful methods. No `@Data` and no uncontrolled setters on entities (ArchUnit-enforced).
Separate persistence models only for concrete reasons (complex legacy mapping, read models
very different from write models, critical domain isolation).

## DTOs, requests, commands and mapping

Request/response DTOs **MAY** be passed to Application Services when it stays simple.
**MUST NOT** create a `Command` class per request by default — use commands when multiple
delivery mechanisms trigger the same use case, the API shape differs from the use case
input, or parameters multiply. Use records, Lombok and builders to reduce boilerplate.

Mapping: prefer explicit factory methods close to the object (`OrderResponse.from(order)`).
Dedicated mapper classes **SHOULD NOT** be the default; MapStruct **MAY** be used for
repetitive many-field mappings.

## Validation

Validate at every relevant boundary: delivery (controllers, consumers, schedulers),
application (preconditions, existence), domain (invariants, transitions), persistence
(constraints, FKs, indexes), integration (incoming/outgoing data). The domain **MUST NOT**
depend on controller validation to remain valid.

## Errors and i18n

Business errors are explicit, specific exceptions (`OrderCannotBeCancelledException`), with a
stable error code and message arguments — never a hardcoded user-facing message as source of
truth. Every API **MUST** have a global `@RestControllerAdvice` handler and a predictable
error structure:

```json
{ "code": "order.cannot-be-cancelled", "message": "...", "fields": [] }
```

User-facing messages **MUST** be internationalized from the beginning
(`messages.properties`, `messages_pt_BR.properties`, ...). Never expose raw enum names as
user-facing labels. The project **MUST** define whether backend, frontend or hybrid resolves
API error messages.

## Repositories

Command repositories are aggregate-oriented and **SHOULD** expose explicit locking methods
(`getRequiredForUpdate(id)`) when concurrency risk exists. Read operations are flexible:
query anything that makes sense (projections, views, SQL). Do not force aggregate purity on
read-only queries. Repository interfaces via Spring Data are natural; do NOT create
interface+`Impl` pairs for internal services — interfaces are for real ports (external
providers, messaging, file storage, notification gateways, AI providers, cache, multiple
implementations).

## Shared code

`shared` **MUST NOT** become a dumping ground. Acceptable: error, validation, security,
observability, pagination, i18n. Prefer small duplication over a bad shared abstraction.

## Dates and timezones

UTC for technical instants. `Instant`/`OffsetDateTime` for real instants; avoid
`LocalDateTime` when timezone matters; `LocalDate` for calendar dates; `Duration` vs `Period`
by meaning. APIs use ISO-8601. Never rely on server default timezone. Timezone-sensitive
rules **MUST** be tested.

## Code style and naming

Readable, explicit, domain-oriented. Business language first; technical suffixes when they
clarify (`OrderController`, `OrderCreatedEvent`, `EtaPredictionProvider`, `S3FileStorage`,
`OrderAccessPolicy`). Avoid vague names (`Manager`, `Helper`, `Util`, `Handler`, `Data`)
unless context makes them precise. Events are named as business facts that happened. Avoid
`ServiceImpl`. Constructor injection only. Streams only when clear; Optional without abuse.

Value Objects when they protect invariants, carry business meaning or group values — never
mechanically for every primitive. Simple enums for simple statuses; enums **MAY** contain
behavior; explicit state machines only when workflow complexity justifies; invalid
transitions throw specific business exceptions; important transitions audited.

## Documentation comments (owner rule, revised)

Javadoc is **REQUIRED** for code that carries business meaning or a contract:

- public module APIs/facades and Application Service public methods;
- domain entities' business methods, domain services, policies;
- business exceptions, integration ports/ACL interfaces;
- any non-obvious logic: validation, orchestration, concurrency, date/time, security.

Javadoc is **NOT required** for: trivial records/DTOs with self-explanatory fields, simple
getters/accessors, controllers that only delegate, and test code. Equivalent rules apply to
other languages (TSDoc, docstrings, KDoc, XML docs).

Comments **MUST** explain intent, contract, constraints, side effects and exceptions — never
restate the name:

```java
/**
 * Cancels an order when the current status allows cancellation.
 * @throws OrderCannotBeCancelledException when the status does not allow cancellation.
 */
public void cancel(CancellationReason reason, String requestedBy) { ... }
```
