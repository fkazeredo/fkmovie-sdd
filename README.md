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
