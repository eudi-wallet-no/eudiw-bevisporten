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

## Delivering work

- **Verify rendered UI before saying done.** Run the app and check the changed surface in a browser, at mobile and desktop widths. State explicitly what you could not verify — never report it as verified.
- **Answer from the codebase first.** Resolve what the code can answer, then ask the user in one collected round — only what cannot be inferred.
- **Measured numbers, not estimates.** Claim a word count, diff size or coverage only after running the command that proves it.
- **Spot the inconsistency yourself.** Compare a visual change against the nearest existing page before delivering: same role, same expression. The reviewer should not be the one who finds the drift.

## Keep these files current

After a large change or a long conversation, note what would have helped from the start and add it in a few lines to the closest instruction or skill. These files are rules, not a session log.

## JIRA-ID, branch and PR

Every task has a JIRA-ID. **Ask for it** if you did not get one — never invent it.

- **Branch:** `<jira-id>`, using the exact casing the user gave, for example `EUW-1234` if that is how it was given. No free text. If the name is taken: `<jira-id>-2`, `<jira-id>-3`.
- **PR title:** `<JIRA-ID>: <tittel på norsk>`, for example `EUW-1234: Ny funksjon`. Without the prefix the `validate-pr-title` check fails.
- **PR description:** Norwegian, short and concrete. No AI phrasing ("Denne PR-en introduserer ..."), no emojis, no self-praise, no summary walls. One line is fine for a trivial change.

```
## Hva og hvorfor
1-2 setninger: Hva endres, og hvorfor?

## Endringer
- Viktigste endringer, ikke alle filer og detaljer.
```
