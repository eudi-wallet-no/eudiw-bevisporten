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

## Continuous improvement

After a larger change or a long conversation, take a brief retrospective before finishing. Capture only reusable lessons and corrections, then update the most relevant instruction or skill in a few lines so future work improves without turning these files into a session log.

---

## Preserve production behaviour

For visual UI work, treat production interaction, state, validation, submission and navigation logic as fixed unless the user explicitly asks to change it. Do not replace control types or interaction patterns because another solution seems better.

If a different solution would improve accessibility or maintainability, describe it as a separate proposal with its benefits and risks. Do not include it in the implementation without an explicit request.

---

## JIRA-ID, branch and PR

All tasks have a JIRA-ID. **Always ask the user for the JIRA-ID** if not provided — do not assume or make one up.

- **Branch name:** `<jira-id>`, e.g. `euw-1234`, whether created via `rename_branch` or directly with git. Do not use free-text descriptions. If the branch already exists, increment: `euw-1234-2`, `euw-1234-3` etc.
- **PR title:** `<JIRA-ID>: <PR title>`, e.g. `EUW-1234: Ny funksjon`. The part after the JIRA-ID prefix must be written in **Norwegian**. Without the prefix, the `validate-pr-title` check in GitHub Actions will fail.
- **PR description:** must be written in Norwegian. Keep it human-friendly, short and concrete. Avoid AI language ("Denne PR-en introduserer ..."), emojis, excessive structure, self-praise, and summary walls. One line is fine for trivial changes.

Template:
```
## Hva og hvorfor
1-2 setninger: Hva endres, og hvorfor?

## Endringer
- Viktigste endringer, ikke alle filer og detaljer.
```
