---
name: proposal
description: "Plan code changes before implementing. Map exact locations, understand impact, pick the simplest path, and deliver a testable plan. Do not implement without approval."
license: Digitaliseringsdirektoratet
allowed-tools: ['view', 'grep', 'glob', 'bash', 'sql']
---

# Proposal Skill

**Purpose:** Deliver a clear, standalone plan for changes before any implementation.

## When to use
- Large or multi-file changes
- Architectural decisions or new patterns
- High-risk changes with unclear scope
- Changes affecting other services/domains

---

## Core Disciplines

1. **Ground Truth First** — Read the files you will actually edit, in the workspace you
   will actually edit them in. A monorepo may have several checkouts of the same repo;
   a stale one silently produces a plan full of files and line numbers that don't exist.

2. **Map First** — Find change locations (`file:line`). Trace who calls in, what flows next, what breaks. Search monorepo for existing patterns.

3. **Hypothesis → Test** — Pick the simplest, most maintainable approach. Plan tests (don't run): name the behavior, expected result, command.

4. **Wait for Approval** — Never code without sign-off. Plan is complete when it answers: What? Where? Why this way? How to verify?

---

## Steps

### 0. Ground truth
Before reading anything, confirm *where* you are reading from:

```bash
pwd                                             # the workspace you will edit
git rev-parse --show-toplevel                   # confirm it's the repo root you expect
git fetch origin <default-branch>
git rev-list --left-right --count HEAD...origin/<default-branch>   # must not be behind
```

If behind, sync before mapping. Then confirm the files exist *in this tree* — `glob` the
directory rather than assuming a path from memory, another checkout, or an earlier session.
Never map or edit via an absolute path outside the current workspace.

### 1. Map
Find all change locations and impacts. Who is affected? Any reusable patterns in the monorepo?

### 2. Scope
What is the goal? What changes, what doesn't? Any blockers?

### 3. Paths
1–2 realistic approaches. Which is simplest and fits existing patterns? Why?

### 4. Plan
Create the tasks as SQL todos (with dependencies), each with a clear test strategy and
done-when condition in its description. Don't duplicate that detail as a table in `plan.md`.

---

## Output

Keep the plan short. Investigate thoroughly, but only surface what's decision-relevant —
this is a proposal to read in seconds, not an audit log.

**Answer:**
- Goal (one sentence)
- Scope (what changes, what doesn't)
- Recommendation (which path, why)
- Related issues / Out of scope (other things worth considering separately)

**Map:** 3–6 bullets max, only what shapes the decision:
- The exact locations that change (`file:line`), grouped, not itemized line-by-line
- Any existing pattern being reused, or why none fits (one line)
- Anything unusual that affects the approach (a gotcha, a shared dependency, a missing test)

Do not list every unchanged line/section of a file "for completeness" — if it isn't
changing and isn't relevant to the decision, leave it out.

**Plan:** Create the tasks as SQL todos (with `todo_deps` for ordering) — don't also render
a task table in `plan.md`. In the plan output, just list task IDs with a one-line goal each;
the full detail (where, impacts, test, done-when) lives in each todo's `description` field.

---

## Non-Negotiable

- **Ground truth before mapping.** Right workspace, synced with the default branch, files
  confirmed to exist in *this* tree. A plan built on a stale checkout is worse than no plan.
- **Map before plan.** Know where the change lives and what it affects.
- **No code claims without file:line** — and only from files you actually opened in this
  workspace. Don't carry over paths or line numbers from another checkout or from memory.
- **Reuse first.** Only design new patterns when existing ones don't fit.
- **Check for a domain skill.** If the change touches an area with its own skill
  (UI/design system, tests, migrations), read it before proposing an approach.
- **Justify deviations.** Breaking from patterns? Explain why.
- **Plan, don't code.** Wait for approval before implementing.

---

## After Approval

1. Execute tasks in dependency order
2. Keep todo status current — set `in_progress` when starting, `done` only once verified.
   Don't leave everything `pending` and mark it all done at the end.
3. Verify against the done-when condition, not just "it ran". For UI, that means rendering
   the page and comparing to the reference; for logic, the named test.
4. Run all tests
5. Update docs if changes made them incorrect

