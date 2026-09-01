---
name: tlc-spec-driven
description: Use BookFlow's repository-local specification workflow for product, UX, architecture, integration, bug-fix, build, or release changes.
---

# BookFlow TLC spec-driven development

TLC means **Initialize → Think → Lock → Code → Verify**. The active spec is the execution source of truth, and work is complete only when its acceptance criteria have evidence.

## 0. Initialize

Initialize before the first change in a new session, when repository/product context is stale, or when work enters an uninspected module. Reuse a valid snapshot during the same unchanged session.

1. Read root `AGENTS.md`, this skill, the current branch and latest relevant commit.
2. Read `docs/BOOKFLOW_BLUEPRINT.md`, active specs and current app/build metadata.
3. Identify affected modules, sibling flows, persistence impact and external dependencies.
4. Record available tests/build/CI as `PASS`, `FAIL`, `NOT RUN` or `UNAVAILABLE`; never infer a pass.
5. Refresh `docs/specs/_initialization.md` with context, constraints and the proposed spec.

Initialization establishes context; it does not replace a feature spec.

## 1. Think

- State the concrete user problem and desired observable result.
- Inspect relevant code and product intent before proposing implementation.
- Check cross-screen, state, lifecycle, accessibility, device-size, file-format, privacy, migration and regression impact where applicable.
- Define non-goals so the task does not grow silently.

## 2. Lock

Before implementation, create or update one focused spec in `docs/specs/` with:

- ID, status and owner.
- Problem and desired behavior.
- Scope, affected modules and non-goals.
- Observable acceptance criteria.
- Data/persistence and UI/UX impact.
- Edge cases, regressions and verification plan.

Do not broaden a locked spec without updating it first.

## 3. Code

- Implement the smallest coherent change that satisfies the locked spec.
- Preserve behavior outside scope and reuse shared solutions for shared defects.
- Add focused tests or deterministic checks where practical.
- Run all available static, unit, build and packaging checks relevant to the change.
- External configuration or credentials may remain `BLOCKED`; never fabricate them or weaken build safety to conceal their absence.

## 4. Verify

- Compare code and observable results against every acceptance criterion.
- Record each criterion as `PASS` or `BLOCKED` with concrete evidence.
- Check affected sibling screens, lifecycle behavior, persistence and regressions.
- Keep physical-device-only behavior `BLOCKED` until device evidence exists.
- A build proves packaging/compilation, not complete UX correctness.

## Definition of Done

Done requires:

1. A valid initialization snapshot.
2. A locked spec.
3. Every criterion marked PASS or explicitly BLOCKED with evidence.
4. Relevant sibling, persistence and regression checks.
5. User-facing documentation updated when needed.
6. Commit or PR message containing `Spec: BF-AREA-NNN` plus verification status.

## Lifecycle and conventions

- Lifecycle: `DRAFT → LOCKED → IMPLEMENTING → VERIFYING → DONE`.
- IDs: `BF-<AREA>-NNN`, for example `BF-CORE-002` or `BF-NARRATION-004`.
- Product intent: current `docs/BOOKFLOW_BLUEPRINT.md` plus explicit user decisions.
- Specs: `docs/specs/`; changelogs are historical evidence, not active requirements.
- Bug fixes include a regression criterion and inspect the same pattern in sibling flows.
- Never attach book text, titles, URIs or user identity to diagnostics unless an explicit privacy-reviewed spec requires it.
