# BookFlow Agent Instructions

All repository changes must follow `.agents/skills/tlc-spec-driven/SKILL.md`.

Before the first code change in a working session:

1. Run Phase 0: Initialize.
2. Produce or refresh the snapshot in `docs/specs/_initialization.md`.
3. Confirm the current branch, app/build version, Blueprint, relevant modules and observable baseline.

Before changing behavior, UX, architecture, integrations or release tooling:

1. Read the relevant code and current BookFlow Blueprint.
2. Create or update a spec under `docs/specs/`.
3. Lock scope, non-goals and observable acceptance criteria.
4. Implement the smallest coherent change.
5. Verify every criterion and record PASS or BLOCKED with evidence.

Preserve working behavior when old documentation and current code disagree unless the active spec explicitly changes it. A successful build is necessary evidence, but is not by itself Definition of Done.
