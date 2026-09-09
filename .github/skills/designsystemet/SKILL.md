---
name: designsystem
description: "Build and change UI with Designsystemet components, tokens and version-matched documentation. Follow WCAG 2.2 AA."
license: Digitaliseringsdirektoratet
---

# Designsystemet

Use [Designsystemet](https://designsystemet.no). If a surface deliberately uses another design system, preserve it and apply the accessibility rules below; do not add Designsystemet during an ordinary fix.

## Rules

- Repair or reuse existing components before creating new ones.
- Use documented components and semantic tokens for their intended purpose. Do not invent tokens. Custom markup or CSS requires a concrete product reason.
- Fix markup, variants and structure before adding CSS.
- Do not upgrade packages, regenerate the theme or add the design system's JavaScript layer during an ordinary visual fix.

## Look it up

Match each loaded package's version; CSS, web, React and theme packages are versioned separately. Read component `overview`, `code` and `accessibility` pages and relevant [patterns](https://designsystemet.no/no/patterns). Example:

```sh
curl -H 'Accept: text/markdown' 'https://designsystemet.no/no/components/docs/button/overview'
```

Look up [tokens](https://designsystemet.no/no/fundamentals/theme/variables) rather than guessing. If current docs do not match, use [source and changelogs](https://github.com/digdir/designsystemet) at the matching tag or installed packages when offline.

CSS provides styling, not keyboard behavior, ARIA wiring or focus management. Check required elements and direct children. React `asChild` needs one child that passes through received props and forwards its ref.

## Accessibility and layout

- Follow [WCAG 2.2 AA](https://www.w3.org/WAI/WCAG22/quickref/). Check accessible names, roles, keyboard access, visible focus without traps, zoom and final foreground/background contrast.
- Check default, hover, focus, invalid and disabled states where applicable, on mobile and desktop.
- Use native elements: `<button>` for actions and `<a href>` for navigation.
- Use semantic structure and labels; do not use ARIA to hide invalid markup. Keep DOM, visual and focus order consistent; never use positive `tabindex`.
- Do not use color alone to convey meaning. If using `aria-disabled`, also prevent the action in code.
- Preserve structure and flow. Let parents control spacing between blocks and components control internal spacing.
- Compare computed spacing and layout with the nearest existing page. Check conditional visibility with and without data and long labels. CSS `display` can override `[hidden]`; bare `fieldset` elements have browser-default borders.
