---
name: designsystem
description: "Build and polish UI with Digitaliseringsdirektoratet's Designsystemet. Use its components and tokens over custom code, look details up in the official docs, and meet WCAG 2.2 AA. Use for anything visual."
license: Digitaliseringsdirektoratet
---

# Designsystemet

All UI follows [Designsystemet](https://designsystemet.no) — the default contract, not a suggestion. On a surface that deliberately uses another design language, preserve that direction and apply the accessibility rules below; never add the dependency as part of an ordinary fix.

Look details up against the version this app loads, never from memory: component structure, roles, tokens and patterns all live in the sources below.

## 1. Use Designsystemet, avoid custom

- Build UI from Designsystemet components and tokens. Custom markup or CSS is the last resort and needs a product-specific reason.
- Take the highest rung that fits: repair the existing component → reuse a shared local composition → use a documented component for the role → compose primitives with theme tokens → build custom.
- Fix markup before adding CSS: an override must never compensate for the wrong component, variant, child structure or missing behaviour layer.
- Use semantic tokens by purpose, never by whichever value looks closest. Do not invent tokens.
- Never upgrade packages, regenerate the theme or add the JavaScript layer as part of an ordinary visual fix.

## 2. Look it up

designsystemet.no serves agent-readable Markdown:

```sh
curl -H 'Accept: text/markdown' https://designsystemet.no/no/components/docs/button/overview
```

| Need | Source |
| --- | --- |
| Which component for this role, and when to avoid it | `/no/components/docs/<component>/overview` |
| Required markup, props, `data-*`, variants, states | `/no/components/docs/<component>/code` |
| Accessible name, roles, keyboard behaviour | `/no/components/docs/<component>/accessibility` |
| Token names and values | [Design tokens](https://designsystemet.no/no/fundamentals/theme/variables) |
| Validation, button placement, system messages | [Patterns](https://designsystemet.no/no/patterns), [Best practices](https://designsystemet.no/no/best-practices) |
| Rendered variants and states | [Storybook](https://storybook.designsystemet.no) |
| Undocumented behaviour, which components need the JS layer, migration history | [Source](https://github.com/digdir/designsystemet) at the matching tag: `packages/*/src`, `packages/*/CHANGELOG.md` |

Read Overview, Code and Accessibility for every component you touch, plus the relevant Pattern for a flow.

**Match the pinned version.** The packages (`@digdir/designsystemet-css`, `@digdir/designsystemet-web`, `@digdir/designsystemet-react`, `@digdir/designsystemet-theme`) are versioned independently and do drift; read each `CHANGELOG.md`, and never apply `main` docs to an older pin. Offline, read the installed package under `node_modules` and the loaded stylesheet, and record which checks you could not run.

Three traps the docs will not warn you about:

- **A class name is not proof.** Selectors depend on direct children, `:has()` and exact tags, so an extra wrapper silently breaks spacing, delegation or state styles. Verify the rendered DOM.
- **CSS, web and React are separate contracts.** CSS styles only; interaction, ARIA wiring and focus come from the `@digdir/designsystemet-web` custom elements or React. Never claim CSS classes provide behaviour. In React, `asChild` takes one child that must spread received props and forward its ref, or handlers and refs drop silently.
- **A token name does not guarantee contrast.** Verify the final foreground/background pair after inheritance and state styles.

## 3. Accessibility

WCAG 2.2 level AA is the floor for every visual change — look criteria up in the [quickref](https://www.w3.org/WAI/WCAG22/quickref/), not from memory. Designsystemet reduces risk but proves nothing; verify with keyboard, screen reader semantics, zoom and contrast. Check every state (default, hover, focus, invalid, disabled) at mobile and desktop widths.

| Never | Instead |
| --- | --- |
| Positive `tabindex` to fix order | Fix source order so DOM, visual and focus order align |
| ARIA to disguise invalid structure | Real labels, headings, lists, tables and landmarks first; native controls before clickable `div` |
| `<div>` click handlers for actions or navigation | `<button>` acts, `<a href>` navigates — whatever they look like |
| `aria-disabled` alone | Avoid disabled actions; if unavoidable, also prevent the action in code |
| Colour alone carrying meaning | Add text, icon or shape |
| Invisible focus or focus traps | Full keyboard path, visible focus, logical tab order |

## 4. Layout

Layout rarely changes — do not redesign it as part of a fix.

- Build pages as blocks of Designsystemet elements, separated by vertical spacing. The parent owns spacing between blocks; a block owns spacing between its children. Never stack margin and padding to patch a gap.
- Favour air and short texts. Title, ingress, heading, label and action each have one job — do not repeat the same message at every level.
- For Norwegian copy use the `norsk-klarsprak` skill; preserve the page's målform and terminology.
