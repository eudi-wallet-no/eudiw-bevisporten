---
name: proposal
description: "Plan code changes before implementing. Challenge the premise, map exact locations, pick the simplest path, and deliver a testable plan. Do not implement without approval."
license: Digitaliseringsdirektoratet
allowed-tools: ['view', 'grep', 'glob', 'bash']
---

# Proposal

Deliver a standalone plan before implementing. Use it for multi-file changes, architectural decisions, new patterns, unclear scope, or anything touching other services. The plan is done when it answers: what, where, why this way, how to verify. Never code without sign-off.

## 1. Size the task

Size once, up front — it decides how heavy the rest of the loop is.

- **Small** — clear scope, one or two files, additive: thin plan (goal, changes, verification). Skip steps 5 and 7 where they add nothing.
- **Medium** — multi-file, new pattern, unclear scope: full loop.
- **Large** — cross-service, architecture, or anything touching auth, payments, schema migrations or core business logic: full loop with deeper mapping, more edge-case hunting and a wider caller search. When runtime behavior changes, the plan also states how it stays observable, what happens when a dependency fails, and where secrets live.

If unsure, treat as Medium.

## 2. Challenge the request

Before planning, test the premise at requirements level: does the change conflict with behavior users depend on, treat a symptom where the codebase points to a different cause, create dangerous edge cases, or rest on an assumption you can disprove? If so, stop and say so with evidence before writing the plan — a wrong premise fixed now is the cheapest save there is. Otherwise stay silent and plan.

When you stop, show the concern and hand the choice to the user:

> ⚠️ **Pushback**: [the concern, with the evidence behind it, and your recommended alternative]

Then `ask_user` with choices: "Proceed as requested" / "Use the recommendation" / "Rethink the task". Do not plan until the user answers.

## 3. Understand

Parse the request into goal, acceptance criteria, material assumptions and open questions. Resolve questions from the codebase first — existing patterns, data models, examples — and ask the user only what cannot be found or inferred. Restate the goal as a precise spec; surface the restatement only when your reading differs materially from the literal ask. Never leave a blocking question for the user to discover during review.

## 4. Ground truth

Read the files you will actually edit, in the workspace you will actually edit them in. A monorepo can hold several checkouts of the same repo, and a stale one yields a plan full of paths and line numbers that do not exist — worse than no plan.

```bash
git rev-parse --show-toplevel      # the repo root you expect
git fetch origin <default-branch>
git rev-list --left-right --count HEAD...origin/<default-branch>   # must not be behind
git status --short                 # uncommitted work under the map?
```

Sync if behind. Note dirty paths — if they overlap the files you will map, say so: attribution and rollback are weak on a poisoned tree. Glob the directory to confirm the files exist in this tree; do not trust a remembered path. Never map or edit outside the current workspace. No code claim without `file:line` — from a file you opened here, never from memory or another checkout.

## 5. Survey

Search before designing: existing code that does something similar, the pattern it set, test infrastructure, blast radius. Map who calls in, what flows next, what breaks.

- **Prior art with a cost.** Open the Options with the closest analog in the monorepo and whether you follow it or deviate: "extending `app/file` is ~15 lines, writing new is ~200". Reuse first; design a new pattern only when none fits, and say why. No analog is itself a finding — the choice is then about establishing a pattern.
- **Settled or mid-migration?** `git log --oneline -5 -- <file>` — an in-progress migration constrains the options more than a settled file.
- **Domain skill.** UI, tests, migrations — read the skill before choosing an approach.

Keep the findings out of the plan unless they shape the decision.

## 6. Plan

Pick the smallest complete change: when options solve the task equally well, prefer the smallest change surface — fewer files, concepts, dependencies, state transitions, integrations. Measure impact, not line count; a smaller patch that hides a symptom, duplicates logic or leaves behavior inconsistent is not smaller.

Create the tasks as SQL todos with dependencies. Each description carries its own test strategy and done-when condition; do not repeat that detail in the plan. Plan tests, do not run them: name the behavior, the expected result and the command.

## 7. Challenge the draft

For non-trivial changes, have the rubber duck agent review the draft plan first, if it is available in your Copilot environment: missing states and behaviors, edge cases, accessibility, integration points, error handling, data consistency. Revise, then present. Skip it for one-file changes with clear scope and no cross-cutting concerns. Requirements are cheapest to fix while the plan is still a plan.

For significant layout or interaction changes, offer ASCII sketches, so a misreading surfaces before implementation rather than after.

## 8. Present

Keep it short — surface only what decides something. The plan answers, in order:

1. **Goal** — the outcome, the acceptance criteria, and the assumptions the plan rests on.
2. **Scope** — what changes, and what deliberately does not.
3. **Options** — prior art first, then the realistic alternative, then the recommendation with the trade-off stated honestly. One line when only one approach is sensible; recommendation never before options.
4. **Technical changes** — the changing locations as grouped `file:line` bullets, high-risk touch points flagged, the pattern reused or why none fits.
5. **Verification** — the real commands, discovered from instruction files and config — never invented.

Tasks are todo IDs with a one-line goal each; the detail lives in the todo.

## After approval

Before the first edit, run the discovered build and test commands once to pin the baseline — a red baseline is noted, not fixed. Then execute in dependency order. Keep todo status current: `in_progress` when you start, `done` only once verified against its done-when condition, not just "it ran". Run the tests. Update documentation the change made wrong.

## Rules

1. **Don't spin.** Two failed attempts to locate a file, pattern or answer — say what failed and ask, instead of guessing.
2. **Parallelize.** Independent searches and reads are batched, never serialized.
3. **Pay knowledge forward.** A convention you discovered and relied on goes into the nearest instruction file, not only into the plan.
