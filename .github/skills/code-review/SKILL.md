# Code review

Review the stable task diff once. Do not edit. Return one concise report, then stop.

## Workflow

1. Read the request and the complete task diff, including tests, documentation, generated files and untracked files. Separate task changes from pre-existing user work.
2. Finish the full pass before reporting. Return all high-confidence findings together; do not stop after the first finding.
3. Check correctness, security, accessibility, regressions and whether every changed line has a concrete task-specific reason.
4. Classify the implementation mentally as obvious code, necessary complexity or clever code:
   - Obvious code can be understood locally and followed without hidden context.
   - Necessary complexity has a concrete requirement or correctness reason.
   - Clever code relies on compressed logic, implicit coupling, several state owners, avoidable indirection, speculative abstractions or unnecessary layers and branches.
5. Report a simplification only when an explicit, behavior-preserving alternative removes a concept, state owner, layer, branch, abstraction, duplicated path or file. Do not report taste, naming preferences or micro-refactoring as simplification.
6. Do not change code or launch a separate simplicity review. Use existing validation results and run an additional check only when needed to confirm a finding.
7. After the report, ask the user which findings to address. Batch approved fixes.
8. Recheck only the addressed findings. Run another full review only when the user requests it or the fixes materially change the architecture.

Preserve production behavior unless the task explicitly changes it.

## Report

Use only the sections that contain findings:

- **Critical** — exploitable security, data-loss or release-blocking defects.
- **Important** — high-confidence incorrect behavior, accessibility defects or regressions that should normally be fixed.
- **Simplification** — concrete, behavior-preserving ways to replace clever code with obvious code.

Keep each finding short: location, consequence and smallest safe direction. Omit low-confidence and style-only observations. If there are no findings, say so in one sentence.

Prefer the reviewer's default fast model for the initial pass. Escalate only a specific critical or cross-cutting finding to a stronger model.
