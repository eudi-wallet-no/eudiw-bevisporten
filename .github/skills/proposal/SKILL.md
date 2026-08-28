---
name: proposal
description: "Plan code changes before implementing. Map exact locations, understand impact, pick the simplest path, and deliver a testable plan. Do not implement without approval."
license: Digitaliseringsdirektoratet
allowed-tools: ['view', 'grep', 'glob', 'bash']
---

# Proposal

Deliver a standalone plan before implementing. Use it for multi-file changes, architectural decisions, new patterns, unclear scope, or anything touching other services.

## Principles

- **Ground truth first.** Read the files you will actually edit, in the workspace you will actually edit them in: right repo root, synced with the default branch, files confirmed to exist in this tree. A monorepo can hold several checkouts of the same repo, and a stale one gives you a plan full of paths and line numbers that do not exist. That is worse than no plan.
- **Map before proposing.** Find the change locations, who calls in, what flows next, what breaks. Keep the findings out of the plan unless they shape the decision.
- **No code claim without `file:line`** — from a file you opened in this workspace, never from memory or another checkout.
- **Reuse first.** Take the simplest approach that fits existing patterns. Design a new pattern only when none fits, and say why.
- **Smallest complete change.** When options solve the task equally well, prefer the smallest change surface: fewer files, concepts, dependencies, state transitions and integrations. Measure impact, not line count. A smaller patch that hides a symptom, duplicates logic or leaves behavior inconsistent is not smaller.
- **Check for a domain skill.** UI, tests, migrations — read the skill before choosing an approach.
- **Plan tests, do not run them.** Name the behavior, the expected result and the command.
- **Never code without sign-off.** The plan is done when it answers: what, where, why this way, how to verify.

## Investigate

Confirm where you are reading from before you read anything:

```bash
git rev-parse --show-toplevel      # the repo root you expect
git fetch origin <default-branch>
git rev-list --left-right --count HEAD...origin/<default-branch>   # must not be behind
```

Sync if you are behind. Glob the directory to confirm the files exist here; do not trust a remembered path. Never map or edit outside the current workspace.

Then map the change locations and their impact, settle the scope, and look for prior art before designing anything:

```bash
git log --oneline -5 -- <file>     # settled area, or mid-migration?
```

Finally create the tasks as SQL todos with dependencies. Each description carries its own test strategy and done-when condition; do not repeat that detail in the plan.

## Rubber Duck Review (optional, if available)

If rubber duck review is enabled in your Copilot environment, use it before presenting the plan for approval to catch missing behavior, state, accessibility, and integration cases:

1. **For non-trivial changes** (multi-file, architectural decisions, new patterns), invoke the rubber duck agent to review the draft plan
2. **Ask about:** missing states or behaviors, edge cases, accessibility requirements, integration points with other features, error handling, and data consistency
3. **Revise the plan** based on findings before presenting it for sign-off
4. **For trivial changes** (one-file, clear scope, no cross-cutting concerns), skip this step and proceed to present

This catches real requirements early, when the plan is cheap to revise—before changes become expensive.

## Present

Keep it short. Investigate thoroughly, but surface only what decides something. No unchanged lines "for completeness". Use this order, so it reads as an argument:

1. **Goal** — one sentence: what outcome, and why.
2. **Scope** — what changes, what does not, what is out of scope for now.
3. **Options** — open with prior art: the closest analog in the monorepo (`app/file`), the convention it set, and whether you follow it or deviate. No analog is itself a finding — the choice is then about establishing a pattern, not picking one. Check when the code last changed; an in-progress migration constrains the options more than a settled file does. Then 1-2 realistic approaches, a couple of lines each, with the trade-off stated honestly. If only one approach is sensible, say so in one line instead of inventing a strawman.
4. **Recommendation** — which option and why. After the options, never before.
5. **Technical changes** — the locations that change (`file:line`, grouped), the pattern being reused or why none fits, and any gotcha that shapes the approach. 3-6 bullets. Filtered mapping, not dumped mapping.
6. **Tasks** — task IDs with a one-line goal each. The detail lives in the todo.
7. **Verification** — how we will know it worked, in one or two lines.

## After approval

Execute in dependency order. Keep todo status current: `in_progress` when you start, `done` only once verified against its done-when condition, not just "it ran". Run the tests. Update documentation the change made wrong.
