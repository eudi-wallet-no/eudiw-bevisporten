---
name: designsystem
description: "Build and polish UI with Digitaliseringsdirektoratet's Designsystemet. Resolve the runtime contract, preserve the product's design direction, repair component, token and accessibility drift, and verify rendered states. Use for anything visual."
license: Digitaliseringsdirektoratet
---

# Designsystemet

All UI follows [Designsystemet](https://designsystemet.no). It is the default UI contract, not a suggestion.

Use Designsystemet components directly in applications that already load it or are explicitly being migrated to it. If a surface intentionally uses another design language or has no Designsystemet runtime, preserve that direction and do not introduce a Designsystemet dependency as part of an ordinary visual fix. Apply the transferable semantic, accessibility and consistency principles instead.

## Mandate

Make the existing design more correct, consistent and accessible without redesigning it. Preserve the product's information architecture, content, behaviour, brand, visual direction and established hierarchy unless the task explicitly changes them.

The requested outcome is always the main job. Complete it first and do not let adjacent cleanup delay, dilute or broaden it.

For a visual change, implement the alignment rather than only reporting it. Inspect the whole affected composition and correct directly coupled Designsystemet violations or accidental drift in the same change, even when the user did not name each one. If an obvious mismatch is safe to fix inside the repair perimeter, fix it instead of merely noting it. Keep that perimeter narrow: the edited component, comparable instances with the same role, shared consumers and the closest reference surface.

A contained correction inside that perimeter can be implemented directly. If it changes a shared component used by several flows, spans applications or requires a layout or interaction decision, follow the repository's proposal rule first.

The result should still look and behave like the same product. Do not introduce a new layout model, component role, colour strategy, density, navigation pattern or visual identity as hidden "polish".

For visual work on an existing flow, production behaviour is the baseline contract. Preserve its control types, interaction model, state transitions, validation, submitted values and navigation unless the request explicitly changes them. A Designsystemet or accessibility alternative may be proposed separately, but must not be substituted into the implementation without explicit approval.

## Two source-of-truth contracts

Designsystemet correctness has two distinct authorities. Do not collapse them into one list.

### 1. Runtime contract: non-negotiable

This decides how a component must be implemented:

1. The Designsystemet packages, theme and scripts the application actually loads.
2. Documentation, component code and accessibility guidance for those versions.
3. The generated theme tokens and documented component custom properties.
4. Package source, types, changelogs and migration notes when documentation is unclear.

A local pattern cannot override an invalid DOM contract, removed API, missing accessible name or required behaviour.

### 2. Product composition: preserve intent

This decides which valid composition belongs in the product:

1. The user's task and explicit product decisions.
2. Shared local components and compositions that already follow the runtime contract.
3. Established patterns with the same role, hierarchy and context in the same flow.
4. The closest comparable surface in the repository.

Upstream examples explain how to use Designsystemet; they do not define this product's page layout. When several valid variants exist, preserve the established local choice. If official sources conflict, prefer the applicable component contract and accessibility outcome, keep existing product behaviour, and do not invent a redesign to resolve the ambiguity.

When the runtime contract and local product patterns do not resolve a composition question, inspect the closest comparable implementation on Digdir.no before inventing a pattern. Compare the element's role, semantics, tokens and states. Use Digdir.no as product evidence, never to override the loaded component contract or an explicit product decision.

## Resolve the runtime before editing

Do this first for every application in scope:

1. **Find the integration.** Check package manifests and lockfiles, CDN URLs, imported stylesheets and scripts, generated theme files, and `designsystemet.config.json`.
2. **Identify the active layers.** Determine whether the app uses:
   - `@digdir/designsystemet-css`;
   - `@digdir/designsystemet-web` custom elements, observers and polyfills;
   - `@digdir/designsystemet-react`;
   - a generated or default theme.
3. **Resolve versions per package.** Read exact dependency or CDN versions, generated CSS build headers and config schema versions. Designsystemet packages can be versioned independently, so consult each relevant changelog instead of requiring identical version numbers.
   - If CSS is vendored without a reliable version marker, do not guess from current documentation. Treat the local stylesheet as the contract and inspect its selectors, attributes and states directly. Use any related theme build version only as supporting evidence.
4. **Use matching documentation.** Do not apply examples from current `main` or the latest Storybook blindly to an older pinned app. Check the matching tag, package source, changelog or migration guide when APIs may differ.
5. **Verify the cascade foundation.** The official setup imports component CSS before the theme. Before reordering an existing layered setup, inspect the declared `ds` layers and change it only when the cascade or runtime behaviour requires it. Place global resets in a layer ordered before `ds`; avoid specificity fights and `!important`.

Do not upgrade Designsystemet, regenerate the theme or add the JavaScript layer as part of an ordinary visual fix unless the task requires it.

The official site can return agent-friendly Markdown:

```sh
rtk curl -H 'Accept: text/markdown' https://designsystemet.no/no/components/docs/button/overview
```

For each component you change, read its **Overview**, **Code** and **Accessibility** pages. Also read a relevant **Pattern** or **Best practice** when the task concerns a flow such as validation, button placement or system messages.

## Reuse and repair ladder

Choose the first level that fully solves the task:

1. Keep the existing Designsystemet component and repair its markup, properties, variant or state.
2. Reuse or improve an existing shared local composition built from valid Designsystemet components.
3. Use a documented Designsystemet component for the role.
4. Compose Designsystemet primitives with theme tokens and existing layout utilities.
5. Build custom only when no documented component or local composition fits.

Custom work must preserve native semantics, use Designsystemet tokens and states, and have a clear product-specific reason. Do not create a parallel class or wrapper for a role that already has a shared composition.

Fix markup before adding CSS. A local override must not compensate for the wrong component, variant, child structure or missing behaviour layer.

## Full component contract

A class name alone is never proof that a component is correct. Verify all of these against the loaded version:

- semantic element or required custom-element tag;
- wrapper and direct-child structure;
- supported class names, properties and `data-*` attributes;
- variant, colour and size inheritance;
- required JavaScript behaviour;
- accessible name, description, state and focus behaviour;
- hover, active, selected, loading, invalid, disabled and read-only states.

Selectors can depend on direct children, `:has()` and exact tags. Extra wrappers can silently break spacing, padding, click delegation or state styles.

## Accessibility baseline

WCAG 2.2 level AA is the minimum acceptance level for every visual change. Designsystemet components and tokens reduce risk but do not prove compliance; verify the rendered composition and custom behaviour.

- Normal text needs at least 4.5:1 contrast and large text at least 3:1. Meaningful component boundaries, controls and state indicators need at least 3:1 where WCAG requires non-text contrast.
- Check foreground and background contrast in default, hover, active, selected, focus, invalid and disabled states. Never rely on colour alone to communicate meaning or interaction.
- Every interaction must work with a keyboard, expose the correct accessible name, role, value and state, and keep a visible focus indicator in logical order.
- Test the complete keyboard path through the page, not only the edited control. Tab and Shift+Tab must follow the reading order without traps or unexpected stops; use arrow keys for native radio groups and other documented composite widgets. Do not use a positive `tabindex` to repair source order.
- Verify the screen-reader contract from the rendered accessibility semantics: descriptive page title and language, landmarks, heading hierarchy, grouped form controls, labels, descriptions, errors, expanded/selected state and live-region updates. ARIA supplements native HTML; it must not disguise an invalid interactive structure.
- Content must reflow without two-dimensional scrolling at 320 CSS pixels where applicable, remain usable when text is enlarged to 200%, and avoid clipping or overlap.
- Pointer targets must meet the WCAG 2.2 AA minimum of 24 by 24 CSS pixels or its spacing exception. Prefer the documented Designsystemet component sizes, which normally provide larger targets.
- Automated accessibility checks supplement rather than replace keyboard, contrast, zoom/reflow and screen-reader-oriented DOM inspection.

### Native semantics first

- Use `<a>` for navigation and `<button>` for actions, regardless of visual appearance.
- Use headings in logical order. Choose visual size independently from the semantic `h1`-`h6` level.
- Use real labels, legends, lists, tables and navigation landmarks before adding ARIA.
- Keep DOM order, visual order and keyboard focus order aligned.
- Decorative icons use `aria-hidden`; icon-only controls need an accessible name that describes the action.

In React, use documented `asChild` composition when changing the rendered element. It accepts one child; custom children must spread received properties and forward their ref. In HTML, apply the documented classes and attributes directly to the correct native element.

### CSS, web and React are different contracts

Designsystemet provides styling through CSS and behaviour through `@digdir/designsystemet-web` custom elements and observers or the React library. CSS-only use is valid only when the application implements and tests the required interaction, ARIA wiring and focus management itself.

Never assume that documented automatic behaviour is active before confirming the relevant layer is loaded. Components such as field, fieldset, error summary, tabs, pagination, dialog and popover can depend on custom elements, observers or polyfills. If that layer is absent, either implement the same accessible outcome within the task's scope or surface the missing contract; do not claim that CSS classes provide it.

### Choose the component by role

- Use Button for an action and Link for navigation. Usually keep one primary action per page or task context.
- Use Card to group content or functionality, not as a generic box around long text or an important message.
- Use Alert for short, important semantic messages, ValidationMessage for a field error, and ErrorSummary for errors that block a page or step.
- Use Field for one control with its label, description, validation and counter. Use Fieldset with a real Legend for a group of fields or choices.
- Prefer native controls and documented Designsystemet compositions over clickable `div` elements or hand-built selectable cards.

Do not silently reorder actions, steps or content as visual cleanup. Follow the documented hierarchy and the established flow; changing the journey is a product decision.

## Forms, validation and feedback

- Group one-of-many choices with Fieldset and Radio. Use the documented `outline` variant when a larger card-like target is needed; keep choices vertical unless the content and available width support a row.
- Do not preselect a radio option without a user or domain reason. Preserve entered values and revealed fields after validation or service errors.
- In React, prefer the documented group hook when available. In CSS-only apps, wire shared names, descriptions, invalid state and group validation explicitly.
- Keep field errors next to their field. When several errors block progress, show an ErrorSummary in the documented location, link each message to its field and move focus as required.
- Avoid introducing disabled actions. Prefer an active submit action that explains validation errors. When disabled or `aria-disabled` is unavoidable, explain why and prevent the action in code; ARIA alone does not disable behaviour.
- Loading controls use the documented loading state and `aria-busy`, while application logic prevents duplicate execution.
- Dynamic status messages use an existing live region with the appropriate `role="status"` or `role="alert"`. Do not combine live-region attributes in ways that cause duplicate announcements.

## Theme and token contract

Use semantic tokens by purpose, not by whichever value looks closest.

### Colour

- Prefer `data-color` and semantic `--ds-color-*` tokens over raw colour values.
- Use Background for page layers, Surface for raised content, Border for edges, Text for text and icons, and Base for solid emphasis.
- `border-subtle` is decorative and must not be the only indication that something is interactive.
- `data-color-scheme` resets colour variables at that boundary. Reapply `data-color` when both a new scheme and a non-default colour are needed.
- Alert, ValidationMessage and ErrorSummary use explicit severity colours and do not inherit arbitrary brand colours.
- Components documented as neutral in the loaded version, such as Dialog or Modal and Tooltip, ignore arbitrary `data-color`. Other cascading components inherit the nearest applicable `data-color`.
- Do not assume that a token name alone guarantees contrast in a custom composition. Verify the final foreground/background pair after inheritance and state styles are applied.

### Size and spacing

- Use `--ds-size-*` tokens for gap, padding and margin. Let a flex or grid parent own spacing between direct children.
- Keep related content closer together than separate sections. Define component, group and section rhythm explicitly instead of relying on browser margins, `<br>`, empty elements or one-off offsets.
- Use a consistent `sm`, `md` or `lg` size mode within a context. Mixing sizes must communicate a real hierarchy, not compensate for layout problems.
- Remember that `data-size` sets size mode for most components, but sets the component's own typography or dimensions for documented exceptions such as Heading, Paragraph, Avatar and Spinner. Verify the loaded version.

### Typography, shape and elevation

- Use Designsystemet typography classes and variables. Semantic heading level and visual heading size are separate decisions.
- Use the configured font through `--ds-font-family`; if the app uses Inter, keep the documented weights and lowercase-l feature setting.
- Use radius, border-width, opacity and shadow tokens. Shadows communicate elevation, not decoration; prefer edges rather than shadows in dark surfaces.
- Keep the product's established icon library. Designsystemet does not require one specific set, but meaningful replacement icons must preserve their meaning.

Do not invent fake Designsystemet tokens. Product-specific layout values such as an established container width, breakpoint, intrinsic media size or aspect ratio may remain local when no token exists. Reuse the existing layout contract and explain any genuinely new value.

## Composition and minimal drift

Standardise elements only when they share role, hierarchy and context:

- Header, main content and footer use the intended shared container edges, max-width and responsive side padding.
- Comparable page intros use the same wrapper, readable text width, heading treatment and internal gap.
- Repeated cards, form groups, action groups and navigation regions use the same component, variant, structure, token rhythm and responsive behaviour.
- Persistent actions or step navigation are separated from the content they complete with a shared composition and semantic border token.
- Mobile may be denser, but keeps the same hierarchy, semantics, states and repeated rhythm.

Before changing a repeated visual role, search the whole application for every comparable instance and state. Explicitly include shared consumers or exclude them because their role differs; do not finish a one-instance fix while equivalent components remain inconsistent.

Classify every finding before changing it:

| Finding | Default action |
| --- | --- |
| Invalid or outdated Designsystemet contract | Repair it |
| Accidental drift between comparable roles | Harmonise it |
| Intentional documented or product-specific variant | Preserve it |
| Established production interaction | Preserve it; propose alternatives separately |
| Ambiguous product or interaction decision | Leave it or ask |

Apply a correction at the narrowest shared level whose consumers should all receive it. Change a shared component only after checking every affected consumer.

## Content coherence

Read the page in visual order. Give the title, ingress, section heading, description, label, validation message and action distinct jobs; do not repeat the same instruction at every level.

When Norwegian UI copy changes, invoke the `norsk-klarsprak` skill. Preserve the page's Bokmål or Nynorsk form, terminology and factual meaning.

## Workflow

Scale the process to the risk. A one-property correction inside an already valid component needs only runtime confirmation, the smallest fix and a focused rendered check. Use the full workflow for shared components, repeated roles, forms and validation, interaction changes or several surfaces.

1. **Set the repair perimeter.** Name the affected surface, comparable roles, shared consumers and out-of-scope product decisions.
2. **Resolve runtime and version.** Identify packages, scripts, theme, import order and matching documentation.
3. **Inspect the baseline.** When a browser or preview is available, inspect the actual UI, DOM, computed styles and active behaviour at relevant mobile and desktop widths. Otherwise verify the rendered markup and the loaded CSS selectors statically, and record which visual or interactive checks could not be run rather than assuming they pass.
4. **Map comparable roles.** Record component, semantic element, DOM structure, variant, colour, size, tokens, parent-owned gaps, states and responsive behaviour.
5. **Classify findings.** Separate contract violations and accidental drift from intentional variants and product decisions.
6. **Choose the smallest valid fix.** Follow the reuse ladder and fix structure before styling.
7. **Implement coherently.** Correct directly coupled token, spacing, state and accessibility drift inside the repair perimeter.
8. **Always zoom out.** Do not judge the result from the edited crop alone. Inspect the entire page or flow at relevant mobile and desktop widths, then compare the section, shared shell and closest equivalent surface. Ask whether the whole result now reads as one coherent Designsystemet-based product. If not, adjust any obvious in-scope drift before finishing.
9. **Verify rendered behaviour.** Compare with an already correct reference and inspect affected shared consumers.
10. **Run a skeptic pass.** Remove changes that introduce a new visual direction, duplicate a contract, broaden scope without evidence or cannot be justified by Designsystemet.

## Verification matrix

A visual task is complete only when the applicable checks that can be run pass. Any unavailable rendered or interactive check remains explicit rather than being reported as verified:

- **Runtime:** loaded package, theme and script versions are understood; no undocumented API or assumed JS behaviour was introduced.
- **Contract:** tag, direct children, attributes, variant and state markup match the loaded component version.
- **Semantics:** keyboard path, focus order, accessible names, labels, descriptions, errors and dynamic announcements work.
- **Screen readers:** page title, language, landmarks, headings, groups and control name/role/value/state form a coherent reading and navigation model; dynamic updates are announced once at the right priority.
- **WCAG:** applicable WCAG 2.2 AA requirements pass, including text and non-text contrast, keyboard access, visible focus, zoom/reflow and target size.
- **Visual:** alignment, wrapping, content hierarchy and token rhythm hold at relevant mobile and desktop widths.
- **States:** hover, focus, active, selected, loading, invalid, disabled/read-only and empty states were checked where relevant.
- **Theme:** semantic colours and surfaces work in every colour scheme the product supports.
- **Consistency:** at least one corrected instance and one comparable reference were checked side by side; shared consumers remain correct.
- **Whole-page fit:** the changed element, surrounding section, shared shell and nearest comparable surface read as one coherent system, with no obvious in-scope Designsystemet drift left behind.
- **Restraint:** no unnecessary custom value, duplicate class, component override or product redesign was introduced.

## Official sources

- [Components](https://designsystemet.no/no/components)
- [Fundamentals](https://designsystemet.no/no/fundamentals)
- [Design tokens](https://designsystemet.no/no/fundamentals/theme/variables)
- [Best practices](https://designsystemet.no/no/best-practices)
- [Patterns](https://designsystemet.no/no/patterns)
- [Storybook](https://storybook.designsystemet.no)
- [GitHub source and migration history](https://github.com/digdir/designsystemet)

Prefer the application's pinned version over the latest examples. Use the official source, component CSS/types and migration notes when a documented contract is uncertain.
