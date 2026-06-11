---
description: Create or update a feature spec in docs/specs
---

Create or update a feature spec for: $ARGUMENTS

1. Read `architecture/workflow.md` (Specs section) and `docs/specs/_TEMPLATE.md`.
2. Check whether a spec already exists for this feature in `docs/specs`. If it
   exists, update it instead of creating a duplicate.
3. If creating a new spec, find the highest `NNNN` in `docs/specs/` and use the
   next sequential number. Filename: `NNNN-kebab-case-title.md`.
4. Fill every applicable section with direct, testable language ("The system
   MUST reject X when Y", "The API MUST return 409 with code z").
5. Do NOT invent business rules, error codes, observability fields or
   acceptance criteria. Anything unknown goes under `Open Questions`, and you
   MUST ask me about the items that block implementation.
6. Show me a summary of: filename, business rules captured, error codes,
   open questions to resolve.
