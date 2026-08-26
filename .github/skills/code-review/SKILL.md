# Code review

Review the full diff before delivering. Fix first. Report only what remains.

## Workflow

1. Read the request and the complete diff: code, tests, docs, generated and untracked files. Separate this task's changes from pre-existing user work.
2. Every changed line must earn its place: required by the task, fixes a defect, preserves correctness, or verifies behavior. Otherwise remove it and keep the old code.
3. Cut incidental cleanup, speculation, duplication, scope creep. Never revert the user's existing work.
4. Fix high-confidence bugs, security, accessibility and regressions directly when the fix is narrow and safe.
5. Run the smallest checks that prove the behavior. Then read the final diff again.
6. Report only unresolved high-confidence issues. Never report what you already fixed.

Preserve production behavior unless told to change it. No style opinions. No clean bill without inspection and passing checks.
