# 0000 - Project Foundation

Status: Draft
Related ADRs: 0001, 0002, 0003

## Goal

Establish the repository structure for `fkmovies-poc` aligned with the
architecture template. Every other spec depends on this skeleton existing.

## Scope

Repository-level artifacts only. Backend and frontend project creation belong
to specs 0001 and 0002.

## Business Context

The repository hosts both the Spring Boot backend and the Angular frontend
plus shared documentation. Both deploy from the same monorepo. Documentation
must be useful for human developers and for Claude Code.

## Business Rules

- Repository content MUST be in English.
- `.env.example` MUST be committed.
- `.env.local` MUST be git-ignored.
- Production code MUST NOT be created in this step.
- Backend module layout (used from 0007 onward): `cinema`, `screening`,
  `pricing`, `booking`, `auth`, `payment`, `notification` under
  `com.fksoft.application.<module>` (see ADR 0001).
- `ScreeningSeat` belongs to the `booking` module (booking owns mutations of
  its status). The endpoint `GET /api/screenings/{id}/seats` lives in
  `booking/api/` despite the URL — URL design does not need to mirror module
  structure 1:1.

## Persistence Changes

Not applicable.

## Validation Rules

- The repository MUST be clonable with no missing references.
- `.env.example` MUST list every variable the application reads, with
  placeholder values and inline comments.

## Tests Required

- Smoke test: `docs/specs/`, `docs/adr/`, `architecture/`, `.claude/` exist
  and follow the template layout.
- Architectural test: when 0001 lands, `ArchitectureTest` and
  `ModularityTests` MUST pass.

## Acceptance Criteria

- `CLAUDE.md`, `architecture/`, `docs/specs/`, `docs/adr/`, `.claude/`,
  `backend/config/`, `README.md` exist and follow the template.
- `.gitignore`, `.env.example`, `compose.yaml` (placeholder) exist.
- `docs/project.md` describes the product in one page (audience, scope,
  glossary).
- No secrets committed; `git secrets` or equivalent scan passes.

## Open Questions

None.

## Out of Scope

- Spring Boot project creation (0001).
- Angular project creation (0002).
- Database schema (0007+).
- WebSocket implementation (0013).
- Business features.
