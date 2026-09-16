# AI instructions

## Before you start

- Read [`docs/`](../docs/) and the [code standard](../docs/CODE_STANDARD.md).
- Read and follow the relevant `SKILL.md`, even if your client has no skill tool:
  - Visual changes: [`designsystem`](skills/designsystemet/SKILL.md).
  - Norwegian text: [`norsk-klarsprak`](skills/norsk-klarsprak/SKILL.md).
  - Multi-file changes, unclear scope, new patterns or architecture decisions: [`proposal`](skills/proposal/SKILL.md) before implementation.
- Resolve questions from code and documentation first. Ask about decisions they cannot answer.

## Make the change

- Make the smallest complete change. Reuse existing patterns and dependencies.
- Preserve existing behavior, flow and accessibility unless the user asks otherwise.
- In visual work, preserve control types, interaction, state, validation, submission and navigation. Propose behavioral changes separately.
- Surface and handle errors explicitly. Do not hide errors or return false success.
- Update documentation that becomes incorrect.
- Write short, direct sentences. Remove filler, repetition and self-praise. No emojis. Preserve the project's terminology.
- Apply Clean Code principles: use descriptive names, keep methods focused and short, and extract helpers when they clarify intent.
- Prefer several small, intention-revealing methods over long methods that mix setup, execution, and assertions.
- Use `private static final` uppercase constants for shared immutable test data. Keep one-use strings local to the method where they are used.
- Prefer `String.formatted(...)` and other clear formatting methods over string concatenation with `+`.
- Keep instructions and skills in English. Add only reusable project rules, not session notes.

## Before delivery

- Run relevant existing tests, builds or lint checks.
- For UI: check the page in a browser on mobile and desktop, compare it with the nearest existing page and add a screenshot to the PR.
- Make diagrams readable in both light and dark mode.
- Report only measured numbers and state clearly what could not be verified.

## Branch and PR

- Ask for the JIRA ID if it is missing. Never invent one.
- Branch: `<jira-id>`, preserving the supplied casing; if taken, use `<jira-id>-2`, then `<jira-id>-3`.
- PR title: `<JIRA-ID>: <Norwegian title>`.
- PR description: short and written in Norwegian:

```md
## Hva og hvorfor
Hva endres, og hvorfor?

## Endringer
- Viktigste endringer.

## Skjermbilde (UI-endringer)
<!-- Skjermbilde av endringen -->
```
