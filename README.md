# fkmovies — Cinema Ticketing (built on the Java Architecture Guideline Template)

Cinema ticketing system. See `docs/project.md` for the product overview and `docs/specs/` for features. The repository follows an owner-driven architecture guideline for Java/Spring Boot + Angular, structured for
Claude Code. Derived from `architecture.md` v1, restructured around three layers:

1. **Always-loaded invariants** — `CLAUDE.md` (~90 lines). Read automatically every session.
   Contains only rules valid for 100% of tasks, plus a Routing Map.
2. **On-demand knowledge** — `architecture/*.md`. Loaded only when the task touches the
   area, via the Routing Map. Keeps context cheap and instruction signal high.
3. **Deterministic enforcement** — rules a tool can check are code, not prose:
   - `backend/src/test/.../ArchitectureTest.java` (ArchUnit) and `ModularityTests.java`
     (Spring Modulith) fail the build on boundary violations, `@Data` entities, `*Impl`
     naming, field injection, controller→repository access.
   - `backend/config/` — Spotless + minimal Checkstyle (pom snippets included).
   - `.claude/settings.json` — permission rules deny destructive commands
     (force push, volume rm, flyway:clean...) and protect secrets; hooks auto-format Java
     after edits and block manual edits to generated files.
   - `.claude/commands/` — `/spec`, `/adr`, `/arch-review`.

## Getting started (backend)

Prerequisites: Java 21, Docker. Maven is provided by the wrapper (`backend/mvnw`).

1. Copy `.env.example` to `.env.local` and set a real `JWT_SECRET` (min 32 chars).
   `.env.local` is git-ignored.
2. Start the database: `docker compose up -d db`
3. Load the env vars into your shell, then run the app from `backend/`:

   PowerShell:

   ```powershell
   Get-Content ..\.env.local | Where-Object { $_ -match '^\s*[^#].*=' } |
     ForEach-Object { $n, $v = $_ -split '=', 2; Set-Item "env:$($n.Trim())" $v.Trim() }
   .\mvnw.cmd spring-boot:run
   ```

   bash:

   ```bash
   set -a; source ../.env.local; set +a
   ./mvnw spring-boot:run
   ```

4. Health checks: <http://localhost:8080/actuator/health/liveness> and
   `/actuator/health/readiness` (returns 503 when the DB is down). Prometheus metrics at
   `/actuator/prometheus`.

Build + all tests (integration tests use Testcontainers, so Docker must be running):

```bash
cd backend
./mvnw spotless:apply   # format
./mvnw verify           # tests + ArchUnit + Modulith + spotless:check + checkstyle
```

## Getting started (frontend)

Prerequisites: Node 22.22.3+ (Angular 22). Stack: Angular 22 standalone + signals,
PrimeNG 21, Tailwind 4, ngx-translate (PT-BR default, EN fallback) — see ADR 0008.

```bash
cd frontend
npm install        # .npmrc uses legacy-peer-deps until PrimeNG ships Angular 22 support
npm start          # serves http://localhost:4200 with /api and /ws proxied to :8080
npm run lint && npm run lint:styles && npm test && npm run build
```

The dev proxy (`frontend/proxy.conf.json`) expects the backend on `http://localhost:8080`.

## Adopting in a new project

1. Copy `CLAUDE.md`, `architecture/`, `docs/`, `.claude/` to the repository root.
2. Copy the two architecture tests into the backend, fixing the root package.
3. Merge `backend/config/pom-snippets.xml` blocks into `pom.xml`; copy `checkstyle.xml`
   to `config/`.
4. `chmod +x .claude/hooks/*.sh` (hooks require `jq`).
5. Adjust the Project commands section of `CLAUDE.md` and the package names in
   `ArchitectureTest.java` / `ModularityTests.java`.
6. Commit everything — `.claude/settings.json` is project-scoped and shared with the team.

## Maintaining the guideline

- Rule changes go through the owner: edit `architecture/` and, when a default changes,
  record an ADR.
- New enforceable rules: prefer adding an ArchUnit rule over adding prose.
- Keep `CLAUDE.md` short. If it grows past ~120 lines, move content to `architecture/`
  and route to it.
