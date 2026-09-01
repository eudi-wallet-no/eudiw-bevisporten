---
name: designsystem
description: "Build and polish UI with Digitaliseringsdirektoratet's Designsystemet. Resolve the runtime contract, preserve the product's design direction, repair component, token and accessibility drift, and verify rendered states. Use for anything visual."
license: Digitaliseringsdirektoratet
---

# Designsystemet

All UI follows [Designsystemet](https://designsystemet.no). It is the default contract, not a suggestion. If a surface deliberately uses another design language, or has no Designsystemet runtime, preserve that direction, do not add the dependency as part of an ordinary fix, and apply the transferable semantics, accessibility and consistency principles instead.

This skill holds only what the documentation cannot know: which version this application loads, and which valid composition belongs in this product. Component structure, roles, tokens and patterns are looked up against the loaded version, never recalled from memory.

## 1. Preserve the design

Make the existing design more correct, consistent and accessible without redesigning it. Information architecture, content, brand, visual identity, visual direction, hierarchy, layout model, component roles, colour strategy, density and navigation pattern stay as they are unless the task changes them, or unless they break the runtime contract, which is repaired rather than preserved. Nothing new is slipped in as "polish".

Production behaviour is the baseline contract: control types, interaction model, state transitions, validation, submitted values and navigation stay as they are unless the request changes them. Propose an alternative separately; never substitute it into the implementation. Do not reorder actions, steps or content as visual cleanup — changing the journey is a product decision.

The requested outcome is the main job. Finish it first, implement it rather than only reporting it, and correct directly coupled drift in the same change — but keep a narrow perimeter: the edited component, comparable instances with the same role, shared consumers, and the closest reference surface. A contained correction goes straight in. Follow the repository's proposal rule first when the change touches a shared component used by several flows, spans applications, or needs a layout or interaction decision.

## 2. Two contracts

**Runtime — non-negotiable.** How a component must be implemented, set by the packages, theme and scripts this application actually loads. A local pattern cannot override an invalid DOM contract, a removed API, a missing accessible name or required behaviour.

**Product composition — preserve intent.** Which valid composition belongs here, set by the user's task, explicit product decisions, shared local compositions and established patterns with the same role and context. Upstream examples show how to use Designsystemet; they do not define this product's layout. When several valid variants exist, keep the established local choice. If official sources conflict, follow the applicable component contract and the accessibility outcome, keep existing behaviour, and do not invent a redesign to settle the ambiguity. Still unresolved: compare role, semantics, tokens and states against the closest comparable surface in this repository, then on [Digdir.no](https://www.digdir.no) — as product evidence only, never to override the loaded contract.

## 3. Resolve the runtime before reading any documentation

1. **Find the integration.** Package manifests and lockfiles, CDN URLs, imported stylesheets and scripts, generated theme files, `designsystemet.config.json`.
2. **Identify the active layers.** `@digdir/designsystemet-css` for styling, `@digdir/designsystemet-web` for custom elements and observers, `@digdir/designsystemet-react`, `@digdir/designsystemet-theme`.
3. **Resolve each package separately.** They are versioned independently and do drift: at v1.21.0 of css, react, web and types, theme was still 1.11.0. Read each relevant `CHANGELOG.md`; never infer a version from a sibling's number.
4. **Treat vendored CSS as the contract** when there is no reliable version marker. Inspect the local stylesheet's selectors, attributes and states rather than guessing from current documentation.
5. **Verify the cascade.** Component CSS imports before the theme, and the declared order is `@layer ds.theme, ds.base, ds.components`. Put global resets in a layer ordered before `ds`; avoid specificity fights and `!important`.

Do not upgrade Designsystemet, regenerate the theme or add the JavaScript layer as part of an ordinary visual fix.

## 4. Look it up, do not remember it

The official site serves agent-readable Markdown:

```sh
curl -H 'Accept: text/markdown' https://designsystemet.no/no/components/docs/button/overview
```

| Question | Source |
| --- | --- |
| Which component for this role, and when to avoid it | `/no/components/docs/<component>/overview` |
| Required markup, properties, `data-*`, variants, states | `/no/components/docs/<component>/code` |
| Accessible name, roles, keyboard behaviour | `/no/components/docs/<component>/accessibility` |
| Token names and values | [Design tokens](https://designsystemet.no/no/fundamentals/theme/variables) |
| Validation, button placement, system messages | [Patterns](https://designsystemet.no/no/patterns), [Best practices](https://designsystemet.no/no/best-practices) |
| Rendered variants and states | [Storybook](https://storybook.designsystemet.no) |
| Behaviour the docs leave unclear, migration history | [Source](https://github.com/digdir/designsystemet) at the matching tag: `packages/*/src`, `packages/*/CHANGELOG.md` |

Read Overview, Code and Accessibility for every component you change, plus the relevant Pattern when the task concerns a flow. Match the application's pinned version; never apply docs from `main` to an older pinned application. Offline, read the installed package under `node_modules` and the loaded stylesheet instead, and record which checks you could not run.

Three things the documentation will not warn you about:

- **A class name is not proof.** Selectors depend on direct children, `:has()` and exact tags, so an extra wrapper silently breaks spacing, padding, click delegation or state styles. Verify the rendered DOM against the loaded contract.
- **CSS, web and React are separate contracts.** CSS gives styling only; behaviour comes from `@digdir/designsystemet-web` custom elements and observers, or from React. `packages/web/src` covers breadcrumbs, details, dialog, error-summary, field, fieldset, focusgroup, pagination, popover, search, suggestion, tabs, toggle-group and tooltip, plus click delegation, invokers and readonly. CSS-only use is valid only when the application implements and tests the interaction, ARIA wiring and focus management itself. If that layer is absent, deliver the same accessible outcome within scope or surface the missing contract; never claim that CSS classes provide it. In React, `asChild` takes exactly one child, which must spread the properties it receives and forward its ref — otherwise Designsystemet's handlers, ARIA wiring and refs are dropped silently.
- **A token name does not guarantee contrast.** Verify the final foreground and background pair after inheritance and state styles.

## 5. Take the smallest fix that holds

1. Repair the existing component's markup, properties, variant or state.
2. Reuse or improve an existing shared local composition.
3. Use a documented Designsystemet component for the role.
4. Compose Designsystemet primitives with theme tokens and existing layout utilities.
5. Build custom only when nothing documented or local fits.

Fix markup before adding CSS: an override must never compensate for the wrong component, variant, child structure or missing behaviour layer. Do not add a parallel class or wrapper for a role that already has a shared composition. Use semantic tokens by purpose, never by whichever value looks closest, and do not invent tokens. Custom work keeps native semantics, uses Designsystemet tokens and states, and needs a product-specific reason. Product-specific layout values, such as an established container width, breakpoint or aspect ratio, may stay local when no token exists.

## 6. Keep the product coherent

Judge the page as one whole. An element that is correct in isolation can still break the page next to its neighbours.

- **Think in blocks.** A block is a semantic group with one job: metadata fields, mode toggle, claims list, preview, form actions. The parent owns spacing between blocks; a block owns spacing between its children. Never let a child reach outside its block with margins, and never stack margins and padding to patch a gap. Persistent actions and step navigation are separated from the content they complete, with a shared composition and a semantic border token.
- **Distance encodes grouping.** Distance inside a group is always smaller than distance between groups, and err towards more space between groups: when blocks blur together the page reads as one undifferentiated mass.
- **Same role, same treatment.** Header, main content and footer share container edges, max-width and responsive side padding. Comparable page intros, cards, form groups, action groups, navigation regions and block arrangements share component, variant, structure, token rhythm and responsive behaviour. Hold one `sm`, `md` or `lg` size mode per context; mixing sizes must express real hierarchy, not patch a layout problem. Keep the product's icon library, and let a replacement icon preserve the original meaning. Mobile may be denser, but keeps the same hierarchy, semantics, states and rhythm.
- **Standardise only on shared role, hierarchy and context.** Before changing a repeated role, search the whole application for every comparable instance and state. Apply the correction at the narrowest shared level whose consumers should all receive it, and change a shared component only after checking every consumer.

Classify every finding before changing it:

| Finding | Default action |
| --- | --- |
| Invalid or outdated Designsystemet contract | Repair it |
| Accidental drift between comparable roles | Harmonise it |
| Intentional documented or product-specific variant | Preserve it |
| Established production interaction | Preserve it; propose alternatives separately |
| Ambiguous product or interaction decision | Leave it or ask |

Title, ingress, section heading, description, label, validation message and action each have a distinct job, so do not repeat the same instruction at every level. When Norwegian copy changes, use the `norsk-klarsprak` skill and preserve the page's målform, terminology and factual meaning.

## 7. Verify what you rendered

WCAG 2.2 level AA is the floor for every visual change. Designsystemet reduces risk but does not prove compliance, and automated checks supplement keyboard, contrast, zoom and DOM inspection rather than replacing them.

- **Contrast** 4.5:1 normal text, 3:1 large text, 3:1 meaningful boundaries, controls and state indicators — across default, hover, active, selected, focus, invalid and disabled. Never let colour alone carry meaning.
- **Keyboard** every interaction works, exposes the correct accessible name, role, value and state, and keeps a visible focus indicator. Tab and Shift+Tab follow reading order without traps. Never repair source order with a positive `tabindex`.
- **Announcements** descriptive title and language, landmarks, heading hierarchy, labels, descriptions, errors, expanded and selected state, live regions announced once at the right priority.
- **Reflow** 320 CSS pixels without two-dimensional scrolling, usable at 200% text, no clipping. Pointer targets 24 by 24 CSS pixels or the spacing exception.

**Native semantics first.** `<a>` navigates and `<button>` acts, whatever they look like. Visual size is a separate decision from the `h1`-`h6` level. Use real labels, legends, lists, tables and landmarks before ARIA; ARIA supplements native HTML and must not disguise an invalid structure. Keep DOM order, visual order and focus order aligned. Prefer native controls over clickable `div` elements. Avoid disabled actions; where `aria-disabled` is unavoidable, prevent the action in code, because ARIA alone does not disable behaviour.

## Workflow

Scale to the risk: a one-property correction inside an already valid component needs only runtime confirmation, the smallest fix and a focused rendered check.

Set the perimeter → resolve runtime and look up the matching documentation → inspect the baseline in a browser at mobile and desktop widths → map comparable roles → classify findings and take the smallest valid fix, structure before styling → implement coherently inside the perimeter → zoom out and judge the whole page, never the edited crop → run a skeptic pass and remove anything that introduces a new visual direction, duplicates a contract or broadens scope without evidence.

**Done when** the markup matches the loaded contract with no assumed API or behaviour; keyboard path, focus order, accessible names and announcements work; applicable WCAG 2.2 AA requirements pass; layout, states and every supported colour scheme hold at mobile and desktop widths; one corrected instance and one comparable reference were checked side by side and shared consumers remain correct; and no unnecessary custom value, duplicate class or override was added. Any rendered or interactive check you could not run stays explicit, never reported as verified.
