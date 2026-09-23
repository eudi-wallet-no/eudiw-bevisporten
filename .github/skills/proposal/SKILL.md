---
name: proposal
description: "Anbefal en løsning og lag en godkjenningsklar plan med teknisk avgrensede oppgaver, avhengigheter og beviskrav. Bruk når planlegging er bestilt eller krav, nye mønstre eller arkitekturvalg må avklares. Ikke ved ren undersøkelse, avklart implementasjon eller utførelse av godkjent plan."
license: Digitaliseringsdirektoratet
---

# Proposal

Own the recommendation, task design and approval, not implementation. Recommend an overall solution and target state, then break the chosen solution into bounded tasks that execution agents can complete and integrate. Ground the plan in the actual codebase and domain. Scale investigation depth and presentation length to risk; even a small plan requires the alternative assessment below, but its presentation can be brief.

## Workflow

1. Read relevant files, project instructions and recent history in the current workspace. Check the branch and uncommitted changes. Fetch `main` and bring this branch up to date before planning; do not overwrite user work. Ask if changes conflict.
2. Map the impact before designing a solution. Trace affected user and system flows end to end: entry points, callers, dependencies, data and state changes, downstream consumers and failure paths. Follow competing solution paths into their surrounding implementation and tests; search hits and filenames alone are not evidence. Identify direct and indirect consequences, including shared behavior, ownership, invariants and contracts that must remain compatible. Continue until affected flows and their boundaries are understood; resolve blocking unknowns before choosing a solution.
3. Find existing solutions, the nearest relevant analogue and tests. Establish how the codebase expresses domain rules, responsibilities, errors and side effects. Use this as the basis for judging alternatives. Load another skill only when a concrete planning decision needs its specialized method; do not preload execution skills. If the request conflicts with requirements or rests on a false assumption, explain the evidence and ask before proceeding.
4. Complete the alternative assessment below before drafting the plan. Choose the smallest complete solution that covers the mapped consequences and fits the established architecture. Prefer reuse over new dependencies, abstractions or parallel implementations. Any added complexity or departure from an established pattern needs a demonstrated gap; do not perpetuate a documented defect merely to match existing code. Exclude unrelated changes.
5. Build a verification plan using the rules below. Cover the affected flows and preserved behavior, not just the edited code. For authentication, payments, migrations or cross-service changes, cover affected callers, dependency failures, how failures are detected and where secrets are stored.
6. Break the recommended solution into dependency-ordered, technically bounded tasks that can be assigned to subagents. Each task must define its outcome, suitable executor profile, owned files/modules, prerequisites, interfaces or shared constraints, completion criteria and relevant checks. Mark tasks that can run in parallel; prevent overlapping ownership and add an integration task where needed. Keep coupled work together and do not split tasks just to increase agent count. Challenge the plan for missed edge cases and integration gaps. For multi-file changes, new patterns or architecture decisions, seek an independent review when available; otherwise make a separate critical pass. Revise from evidence.
7. Present the recommendation and target state first, followed by rationale and a concise evidence-based comparison of alternatives. Include scope, affected flows, acceptance criteria, assumptions, uncertainties, tasks and verification. Cite affected locations as `file:line` from files you read; separate facts from assumptions.
8. Wait for approval unless the user has already approved the plan or explicitly authorized its concrete changes. After approval, hand off the agreed tasks to the available execution agents or coordinator with ownership, dependencies, interfaces, constraints, baseline commands and acceptance criteria. Do not depend on a particular installed execution skill. Include affected documentation and final integration checks. Report pre-existing failures without fixing unrelated issues.

Ask instead of guessing after two failed attempts to find a file or answer. Batch independent reads and searches.

## Plan structure

1. **Recommendation:** proposed solution and the overall target state.
2. **Why this solution:** decisive evidence, alternatives considered, and the main trade-off.
3. **Scope:** included and excluded changes, acceptance criteria, affected flows, assumptions and unresolved questions.
4. **Execution tasks:** use as many tasks as the solution needs. Make each independently assignable to one subagent when possible. For each, state its outcome, suitable executor profile, file/module ownership, dependencies, handoff or interface, acceptance criteria and checks. Mark parallel work explicitly; avoid overlapping ownership.
5. **Integration:** identify who or what combines the task results and the end-to-end checks that establish the overall goal.

Tasks must be concrete enough to assign separately without losing the overall goal or requiring the agent to rediscover the plan. When one task cannot be separated safely, keep it together and explain the dependency.

## Verification plan

- Select the smallest set of checks that covers the mapped impact. For each changed behaviour, state what must be proven, relevant normal and failure cases, the project command and expected result, and why the selection is sufficient. Include affected callers, shared behaviour and integrations; changed files alone do not define the test scope. For text-only changes, use the shared rules for diff, format and reference checks.
- During planning, run checks only to resolve a concrete uncertainty or establish a necessary baseline. Otherwise schedule them for implementation. Discover available test commands and selectors from project configuration and existing tests; do not run the full suite merely to identify them.
- Separate focused regression and module tests used during edits from broader integration, type/build or end-to-end checks needed for completion. Consolidate broader checks at a meaningful integration or completion point, covering the final relevant state. Reuse valid results and determine reruns under the shared control rules.
- State when the selection must expand: shared infrastructure changes, uncertain dependencies, unexpected failures or missing coverage. Use a broader set or the full suite when the impact cannot be bounded reliably. Project-mandated checks and high-risk verification requirements still apply; elapsed time alone is not a reason to omit them.
- Carry forward completed checks with their command, checked state and actual result, separately from checks still planned. Define completion by satisfied evidence requirements and resolved material gaps. Do not add another round without a specific reason; report what remains unverified.

## Alternative assessment

1. Investigate at least three distinct, codebase-grounded solution candidates. They must differ meaningfully in change location, responsibility or mechanism, not just naming or syntax. Include the most direct use or extension of an established mechanism. Do not invent unnecessary rewrites or weak options to make a preferred solution win. If investigation rules out a candidate, record the concrete constraint; an examined alternative need not remain viable.
2. Establish the strongest evidence-backed case for each candidate and its main limitation. Compare all candidates against the same criteria: domain and architectural fit; correctness in normal and failure paths; affected dependencies, contracts and change scope; readability, testability and maintenance cost. Distinguish implementation effort from lasting complexity.
3. Challenge the arguments, especially those supporting the provisional favourite. Check whether the claimed benefit depends on an unverified assumption, moves complexity elsewhere, duplicates a rule or leaves a caller or failure path uncovered. Identify what evidence would change the recommendation, then inspect the relevant code or tests. Revise the comparison when findings contradict it. Repeating an argument is not additional evidence.
4. Prefer the obvious solution for someone maintaining this codebase: clear domain names, cohesive responsibilities, a readable control flow and explicit handling of errors and side effects. Clean code means the behaviour is easy to locate, understand, test and change; neither the fewest lines nor the most abstractions establishes this. Conclude when the decisive premises are supported and material objections are resolved. State remaining uncertainty; if it could change the choice, resolve it with a focused check or necessary clarification before recommending a plan.
