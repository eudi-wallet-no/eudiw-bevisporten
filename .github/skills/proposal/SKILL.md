---
name: proposal
description: "Plan code changes before implementing. Map exact locations, understand impact, pick the simplest path, and deliver a testable plan. Do not implement without approval."
license: Digitaliseringsdirektoratet
allowed-tools: ['view', 'grep', 'glob', 'bash']
---

# Proposal Skill

**Purpose:** Deliver a clear, standalone plan for changes before any implementation.

## When to use
- Large or multi-file changes
- Architectural decisions or new patterns
- High-risk changes with unclear scope
- Changes affecting other services/domains

---

## Principles

- **Ground truth first.** Read the files you will actually edit, in the workspace you will actually edit them in: right workspace, synced with the default branch, files confirmed to exist in *this* tree. A monorepo may have several checkouts of the same repo, and a stale one silently produces a plan full of files and line numbers that don't exist — worse than no plan.
- **Map before proposing.** Find change locations (`file:line`). Trace who calls in, what flows next, what breaks. Search the monorepo for existing patterns. Keep the findings out of the document unless they shape the decision.
- **No code claims without `file:line`** — and only from files you actually opened in this workspace. Don't carry over paths or line numbers from another checkout or from memory.
- **Reuse first.** Pick the simplest, most maintainable approach that fits existing patterns. Only design new patterns when existing ones don't fit, and explain any deviation.
- **Smallest complete change.** When several options solve the task equally well, prefer the smallest justified change surface: fewer files, concepts, dependencies, state transitions and integrations. Measure impact, not raw line count. Do not prefer a smaller patch when it only hides a symptom, duplicates logic or leaves behavior inconsistent.
- **Check for a domain skill.** If the change touches an area with its own skill (UI/design system, tests, migrations), read it before proposing an approach.
- **Hypothesis → test.** Plan tests, don't run them: name the behavior, the expected result, and the command.
- **Plan, don't code.** Never code without sign-off. The plan is complete when it answers: What? Where? Why this way? How to verify?

---

## Steps

These are working steps — the order you *investigate* in. It is not the order you
*present* in (see Output).

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
Find all change locations and impacts. Who is affected? Any reusable patterns in the monorepo? Mapping is research, not a deliverable — most of what you find exists only to make the options and the technical changes correct.

### 2. Scope
What is the goal? What changes, what doesn't? Any blockers?

### 3. Options
Start with prior art: search the monorepo for the closest analog before designing anything.

```bash
git log --oneline -5 -- <file>          # is this area settled or mid-migration?
```

Then 1–2 realistic approaches. Which is simplest and fits existing patterns? Why?

### 4. Tasks
Create the tasks as SQL todos (with dependencies), each with a clear test strategy and
done-when condition in its description. Don't duplicate that detail as a table in `plan.md`.

---

## Output

Keep the plan short. Investigate thoroughly, but only surface what's decision-relevant —
this is a proposal to read in seconds, not an audit log. Do not list unchanged lines or
sections "for completeness".

Present it in this order, so it reads as an argument: *why → what's in play → what we could
do → what we should do → what that means in code → what gets done.*

**1. Goal** — one sentence. What outcome, and why.

**2. Scope** — what changes, what explicitly doesn't, what's out of scope for now.

**3. Options** — open with a short paragraph on **prior art**: has the monorepo solved
something like this before? Name the closest analog (`app/file`), what convention it
established, and whether we follow it or deviate. If there is no analog, say so — that is
itself a finding, and means the options are about *establishing* a pattern, not picking one.
Check when the surrounding code last changed; landing on top of an in-progress migration
constrains the options more than an old, settled file does. Then the 1–2 realistic
approaches, each in a couple of lines, with the trade-off stated honestly. If there is
genuinely only one sensible approach, say so in one line — don't invent a strawman.

**4. Recommendation** — which option, and the reason. Comes *after* the options, never before.

**5. Technical changes** — what this means in code: the locations that change
(`file:line`, grouped), the pattern being reused or why none fits, and any gotcha that
shapes the approach. 3–6 bullets. This is where mapping surfaces — filtered, not dumped.

**6. Tasks** — task IDs with a one-line goal each. The full detail (where, impacts, test,
done-when) lives in the todo `description`, not here.

**7. Verification** — how we'll know it worked, in one or two lines.

---

## After Approval

1. Execute tasks in dependency order
2. Keep todo status current — set `in_progress` when starting, `done` only once verified.
   Don't leave everything `pending` and mark it all done at the end.
3. Verify against the done-when condition, not just "it ran". For UI, that means rendering
   the page and comparing to the reference; for logic, the named test.
4. Run all tests
5. Update docs if changes made them incorrect
