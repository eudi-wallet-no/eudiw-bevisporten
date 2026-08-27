# AI Instructions

## Before you start

- Read [`docs/`](../docs/) for the system, roles and architecture. All code follows the [code standard](../docs/CODE_STANDARD.md).
- Anything visual: use the [`designsystem`](skills/designsystemet/SKILL.md) skill. UI follows [Designsystemet](https://designsystemet.no) — reuse its components and tokens instead of writing custom CSS.
- Norwegian UI copy, documentation or product text: use the [`norsk-klarsprak`](skills/norsk-klarsprak/SKILL.md) skill.
- Non-trivial change (multi-file, unclear scope, or a design decision): use the [`proposal`](skills/proposal/SKILL.md) skill as the first action, not after feedback. Sync with `main` first, so the plan is not based on stale history.
- Before delivering code: use the [`code-review`](skills/code-review/SKILL.md) skill once on the stable diff. It is read-only and reports everything at once. Ask before applying fixes, then recheck only what was addressed.

## Writing

Short, direct, plain language. No emojis, no AI phrasing, no structure for its own sake. Diagrams must work in both dark and light mode. Update documentation your change made wrong.

## Preserve production behaviour

In visual work, interaction, state, validation, submission and navigation are fixed unless the user asks to change them. Do not swap control types or interaction patterns because another solution looks better. A better accessibility or maintainability option is a separate proposal with its benefits and risks — never slipped into the implementation.

## Keep these files current

After a large change or a long conversation, note what would have helped from the start and add it in a few lines to the closest instruction or skill. These files are rules, not a session log.

## JIRA-ID, branch and PR

Every task has a JIRA-ID. **Ask for it** if you did not get one — never invent it.

- **Branch:** `<jira-id>`, for example `euw-1234`. No free text. If the name is taken: `euw-1234-2`, `euw-1234-3`.
- **PR title:** `<JIRA-ID>: <tittel på norsk>`, for example `EUW-1234: Ny funksjon`. Without the prefix the `validate-pr-title` check fails.
- **PR description:** Norwegian, short and concrete. No AI phrasing ("Denne PR-en introduserer ..."), no emojis, no self-praise, no summary walls. One line is fine for a trivial change.

```
## Hva og hvorfor
1-2 setninger: Hva endres, og hvorfor?

## Endringer
- Viktigste endringer, ikke alle filer og detaljer.
```
