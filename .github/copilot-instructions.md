# AI Instructions

## Context

Before any work, read [`docs/`](../docs/) to understand the system, roles, and architecture.

## Design System

All UI follows [Digitaliseringsdirektoratet's Designsystemet](https://designsystemet.no). Reuse its components and design tokens. Avoid writing custom CSS.

See [the designsystem skill](skills/designsystem-skill.md) before changing anything visual.

## After completing work

When planning or executing changes, check if documentation needs updating. If your changes make something in the docs incorrect or outdated, update it.

---

## Writing Guidelines

**Keep it simple** — write short, direct, clear content without unnecessary jargon.

- No emojis (documentation, diagrams, PRs)
- No AI language or excessive structure
- Dark/light mode friendly diagrams (no hard-coded colors)

---

## JIRA-ID in branch and PR title

All tasks have a JIRA-ID. **Always ask the user for the JIRA-ID** if not provided — do not assume or make one up. Used in PR title:

```
<JIRA-ID>: <PR title>
```

E.g. `EUW-1234: New feature`. Without this, the `validate-pr-title` check in GitHub Actions will fail.

PR title (the part after the JIRA-ID prefix) should be written in **English**.

## Branch name

New branch (whether created via `rename_branch` or directly with git) should be named `<jira-id>`, e.g. `euw-1234`. Do not use free-text descriptions. If the branch already exists, increment: `euw-1234-2`, `euw-1234-3` etc.

# PR descriptions

Write in human-friendly, short and concrete style. Avoid AI language ("This PR introduces..."), emojis, excessive structure, self-praise, and summary walls. One line is fine for trivial changes.

Template:
```
## What and why
1-2 sentences: what changes and why.

## Changes
- Key points, not every file/detail.
```
