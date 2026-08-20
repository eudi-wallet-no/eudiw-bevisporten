---
name: proposal
description: "Plan code changes before implementing. Map exact locations, understand impact, pick the simplest path, and deliver a testable plan. Do not implement without approval."
license: Digitaliseringsdirektoratet
allowed-tools: ['view', 'grep', 'bash']
---

# Proposal Skill

**Purpose:** Deliver a clear, standalone plan for changes before any implementation.

## When to use
- Large or multi-file changes
- Architectural decisions or new patterns
- High-risk changes with unclear scope
- Changes affecting other services/domains

---

## Three Core Disciplines

1. **Map First** — Find change locations (`file:line`). Trace who calls in, what flows next, what breaks. Search monorepo for existing patterns.

2. **Hypothesis → Test** — Pick the simplest, most maintainable approach. Plan tests (don't run): name the behavior, expected result, command.

3. **Wait for Approval** — Never code without sign-off. Plan is complete when it answers: What? Where? Why this way? How to verify?

---

## Steps

### 1. Map
Find all change locations and impacts. Who is affected? Any reusable patterns in the monorepo?

### 2. Scope
What is the goal? What changes, what doesn't? Any blockers?

### 3. Paths
1–2 realistic approaches. Which is simplest and fits existing patterns? Why?

### 4. Plan
Build task table with dependencies and test strategy.

---

## Output

**Answer:**
- Goal (one sentence)
- Scope (what changes, what doesn't)
- Recommendation (which path, why)
- Related issues / Out of scope (other things worth considering separately)

**Map:**
- Change locations (file:line)
- Flow around changes, who is impacted
- Monorepo analogs, reuse decisions

**Plan:**
```
ID   | Goal           | Where@file:line | Impacts    | Depends | Test          | Done when
task-1| Fix oauth      | oauth:142      | login,byob | -       | Token in req  | Tests pass
```

---

## Non-Negotiable

- **Map before plan.** Know where the change lives and what it affects.
- **No code claims without file:line.**
- **Reuse first.** Only design new patterns when existing ones don't fit.
- **Justify deviations.** Breaking from patterns? Explain why.
- **Plan, don't code.** Wait for approval before implementing.

---

## After Approval

1. Execute tasks in dependency order
2. Run all tests
3. Update docs if changes made them incorrect

