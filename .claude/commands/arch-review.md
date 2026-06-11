---
description: Run an architecture review and produce a findings report
---

Run an architecture review on: $ARGUMENTS (default: code changed in the current branch
versus develop/main).

1. Run the executable checks first and report their raw result:
   - `mvn -q test -Dtest='ArchitectureTest,ModularityTests'`
   - `mvn -q spotless:check checkstyle:check`
2. Read the `architecture/` docs relevant to the reviewed area (use the Routing Map in
   CLAUDE.md) and review the code against them — especially the rules tools cannot check:
   anemic entities, business rules leaking into Application Services, vendor DTOs leaking
   past ACLs, missing Outbox on important events, missing timeouts, JSON contract changes,
   missing i18n, missing regression tests.
3. Produce a report with: finding, severity (critical/major/minor), violated rule (file and
   section in `architecture/`), and recommended fix.
4. Do NOT fix anything in this command — report only. Suggest which findings deserve
   immediate fixes, separate tasks, or an ADR-backed exception.
