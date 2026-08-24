# AI Instructions

## Before you start

- Read [`docs/`](../docs/) to understand the system, roles, and architecture.
- Anything visual: use the [`designsystem` skill](skills/designsystemet/SKILL.md). All UI follows [Digitaliseringsdirektoratet's Designsystemet](https://designsystemet.no) — reuse its components and design tokens instead of writing custom CSS.
- Norwegian UI copy, documentation, or product text: use the [`norsk-klarsprak` skill](skills/norsk-klarsprak/SKILL.md).
- Non-trivial change (multi-file, unclear scope, or a design/architecture decision): use the [`proposal` skill](skills/proposal/SKILL.md) immediately — as the first action, not after feedback. Sync the branch with the latest default branch (`main`) first, so the plan is never based on stale history.

---

## Writing Guidelines

**Keep it simple** — write short, direct, clear content without unnecessary jargon.

- No emojis (documentation, diagrams, PRs)
- No AI language or excessive structure
- Dark/light mode friendly diagrams (no hard-coded colors)
- Update documentation your changes made incorrect or outdated

---

## JIRA-ID, branch and PR

All tasks have a JIRA-ID. **Always ask the user for the JIRA-ID** if not provided — do not assume or make one up.

- **Branch name:** `<jira-id>`, e.g. `euw-1234`, whether created via `rename_branch` or directly with git. Do not use free-text descriptions. If the branch already exists, increment: `euw-1234-2`, `euw-1234-3` etc.
- **PR title:** `<JIRA-ID>: <PR title>`, e.g. `EUW-1234: New feature`. The part after the JIRA-ID prefix is written in **English**. Without the prefix, the `validate-pr-title` check in GitHub Actions will fail.
- **PR description:** human-friendly, short and concrete. Avoid AI language ("This PR introduces..."), emojis, excessive structure, self-praise, and summary walls. One line is fine for trivial changes.

Template:
```
## What and why
1-2 sentences: what changes and why.

## Changes
- Key points, not every file/detail.
```
