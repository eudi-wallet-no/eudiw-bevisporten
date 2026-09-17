# AI instructions

## Before you start

- Read [`docs/`](../docs/) and follow [code standard](../docs/CODE_STANDARD.md) when writing code.
- Read and follow the relevant `SKILL.md`, even if your client has no skill tool:
  - Visual changes: [`designsystem`](skills/designsystemet/SKILL.md).
  - Norwegian text: [`norsk-klarsprak`](skills/norsk-klarsprak/SKILL.md).
  - Multi-file changes, unclear scope, new patterns or architecture decisions: [`proposal`](skills/proposal/SKILL.md) before implementation.

## Make the change

- Surface and handle errors explicitly. Do not hide errors or return false success.
- Update documentation that becomes incorrect.
- Keep instructions and skills in English. Add only reusable project rules, not session notes.

## Before delivery

- Run relevant existing tests, builds or lint checks.
- Make diagrams readable in both light and dark mode.
- Report only measured numbers and state clearly what could not be verified.

## Branch and PR

- Ask for the JIRA ID if it is missing. Never invent one.
- Never create a pull request on the remote. The user creates PRs manually.
- Branch: `<jira-id>`, preserving the supplied casing; if taken, use `<jira-id>-2`, then `<jira-id>-3`.
- PR title: `<JIRA-ID>: <Norwegian title>`.
- PR description: very short and written in Norwegian. Describe the changes in a few plain words; no technical details or file references.
- In `Endringer`, show the whole before the parts: group related changes into categories (e.g. admin, framsida) with one summarized bullet each; only standalone changes get their own bullet.

```md
## Bakgrunn
Hva endres, og hvorfor?

## Endringer
- Viktigste endringer.

## Skjermbilde (UI-endringer)
<!-- Skjermbilde av endringen -->
```
