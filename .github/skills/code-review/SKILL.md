---
name: code-review
description: "Review the stable task diff once and report every high-confidence defect and concrete simplification together. Read-only: never edit code."
license: Digitaliseringsdirektoratet
allowed-tools: ['view', 'grep', 'glob', 'bash', 'ask_user']
---

# Code review

Review the stable task diff once. Do not edit. Return one report, then stop.

## Workflow

1. Read the request and the whole diff: code, tests, docs, generated and untracked files. Separate task changes from pre-existing user work.
2. Finish the full pass before reporting. Report every high-confidence finding together; do not stop at the first one.
3. Check correctness, security, accessibility, regressions, and whether every changed line has a concrete task-specific reason.
4. Classify the implementation:
   - **Obvious** — understandable locally, no hidden context.
   - **Necessary complexity** — a concrete requirement or correctness reason.
   - **Clever** — compressed logic, implicit coupling, several state owners, avoidable indirection, speculative abstraction, extra layers or branches.
5. Report a simplification only when a concrete, behavior-preserving alternative removes a concept, state owner, layer, branch, abstraction, duplicated path or file. Taste, naming and micro-refactoring are not simplifications.
6. Use existing validation results. Run an extra check only to confirm a finding.
7. Ask which findings to address, batch the approved fixes, and recheck only those. Review the whole diff again only on request, or if the fixes changed the architecture.

Preserve production behavior unless the task changes it.

## Report

Use only the sections that have findings:

- **Critical** — exploitable security, data loss, or release blockers.
- **Important** — incorrect behavior, accessibility defects or regressions that should be fixed.
- **Simplification** — concrete, behavior-preserving ways to replace clever code with obvious code.

Per finding: location, consequence, smallest safe direction. Drop low-confidence and style-only notes. If there are no findings, say so in one sentence.

Use the default fast model. Escalate only a specific critical or cross-cutting finding.
