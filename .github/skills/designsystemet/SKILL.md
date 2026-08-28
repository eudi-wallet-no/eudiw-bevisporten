---
name: designsystem
description: "Build and polish UI with Digitaliseringsdirektoratet's Designsystemet. Resolve the runtime contract, preserve the product's design direction, repair component, token and accessibility drift, and verify rendered states. Use for anything visual."
license: Digitaliseringsdirektoratet
---

# Designsystemet

All UI follows [Designsystemet](https://designsystemet.no). It is the default contract, not a suggestion.

Use its components directly in applications that already load it or are being migrated to it. If a surface deliberately uses another design language, or has no Designsystemet runtime, preserve that direction and do not add the dependency as part of an ordinary fix. Apply the transferable semantics, accessibility and consistency principles instead.

## Mandate

Make the existing design more correct, consistent and accessible without redesigning it. Preserve information architecture, content, behaviour, brand, visual direction and hierarchy unless the task changes them. The result must still look and behave like the same product: no new layout model, component role, colour strategy, density, navigation pattern or visual identity slipped in as "polish".

The requested outcome is the main job. Finish it first, and do not let adjacent cleanup dilute or delay it.

Implement the fix, do not only report it. Inspect the whole affected composition and correct directly coupled violations and drift in the same change, even where the user did not name them. Keep the perimeter narrow: the edited component, comparable instances with the same role, shared consumers, and the closest reference surface. A contained correction inside that perimeter goes straight in. If it changes a shared component used by several flows, spans applications, or needs a layout or interaction decision, follow the repository's proposal rule first.

Production behaviour is the baseline contract. Control types, interaction model, state transitions, validation, submitted values and navigation stay as they are unless the request changes them. Propose an alternative separately; never substitute it into the implementation.

## Two contracts

**Runtime — non-negotiable.** How a component must be implemented: the packages, theme and scripts the application actually loads; the documentation, component code and accessibility guidance for those versions; the generated theme tokens and documented custom properties; and package source, types, changelogs and migration notes when the documentation is unclear. A local pattern cannot override an invalid DOM contract, a removed API, a missing accessible name or required behaviour.

**Product composition — preserve intent.** Which valid composition belongs here: the user's task and explicit product decisions; shared local compositions that already follow the runtime contract; established patterns with the same role, hierarchy and context; and the closest comparable surface in the repository. Upstream examples show how to use Designsystemet, but do not define this product's layout. When several valid variants exist, keep the established local choice. If official sources conflict, follow the applicable component contract and the accessibility outcome, keep existing behaviour, and do not invent a redesign to settle the ambiguity. Still unresolved: compare role, semantics, tokens and states against the closest surface on Digdir.no, as product evidence only, never to override the loaded contract.

## Resolve the runtime first

1. **Find the integration.** Package manifests and lockfiles, CDN URLs, imported stylesheets and scripts, generated theme files, `designsystemet.config.json`.
2. **Identify the active layers.** `@digdir/designsystemet-css`, `@digdir/designsystemet-web` custom elements and observers, `@digdir/designsystemet-react`, and whether the theme is generated or default.
3. **Resolve the version per package.** Packages are versioned independently, so read each relevant changelog instead of expecting matching numbers. If CSS is vendored without a reliable version marker, do not guess from current documentation: treat the local stylesheet as the contract and inspect its selectors, attributes and states directly.
4. **Use matching documentation.** Do not apply examples from `main` or the latest Storybook to an older pinned application. Check the matching tag, package source, changelog or migration guide.
5. **Verify the cascade.** The official setup imports component CSS before the theme. Inspect the declared `ds` layers before reordering anything. Put global resets in a layer ordered before `ds`, and avoid specificity fights and `!important`.

Do not upgrade Designsystemet, regenerate the theme or add the JavaScript layer as part of an ordinary visual fix.

The official site serves agent-readable Markdown:

```sh
curl -H 'Accept: text/markdown' https://designsystemet.no/no/components/docs/button/overview
```

Read **Overview**, **Code** and **Accessibility** for every component you change, plus the relevant **Pattern** or **Best practice** when the task concerns a flow such as validation, button placement or system messages.

## Reuse ladder

Take the first level that fully solves the task:

1. Repair the existing component's markup, properties, variant or state.
2. Reuse or improve an existing shared local composition.
3. Use a documented Designsystemet component for the role.
4. Compose Designsystemet primitives with theme tokens and existing layout utilities.
5. Build custom only when nothing documented or local fits.

Custom work keeps native semantics, uses Designsystemet tokens and states, and needs a product-specific reason. Do not add a parallel class or wrapper for a role that already has a shared composition. Fix markup before adding CSS: an override must never compensate for the wrong component, variant, child structure or missing behaviour layer.

## Component contract

A class name is not proof. Against the loaded version, verify the semantic element or required custom-element tag; the wrapper and direct-child structure; supported classes, properties and `data-*` attributes; variant, colour and size inheritance; required JavaScript; accessible name, description, state and focus; and the hover, active, selected, loading, invalid, disabled and read-only states.

Selectors can depend on direct children, `:has()` and exact tags. An extra wrapper silently breaks spacing, padding, click delegation or state styles.

**CSS, web and React are separate contracts.** CSS gives styling; `@digdir/designsystemet-web` custom elements and observers, or the React library, give behaviour. CSS-only use is valid only when the application implements and tests the interaction, ARIA wiring and focus management itself. Field, fieldset, error summary, tabs, pagination, dialog and popover can all depend on custom elements, observers or polyfills. If that layer is absent, either deliver the same accessible outcome within scope or surface the missing contract; never claim that CSS classes provide it.

**Choose by role.** Button for actions, Link for navigation, usually one primary action per task context. Card groups content or functionality; it is not a generic box around long text or an important message. Alert for short important messages, ValidationMessage for a field error, ErrorSummary for errors blocking a page or step. Field for one control with its label, description, validation and counter; Fieldset with a real Legend for a group. Prefer native controls and documented compositions over clickable `div` elements or hand-built selectable cards. Do not reorder actions, steps or content as visual cleanup — changing the journey is a product decision.

## Accessibility

WCAG 2.2 level AA is the minimum for every visual change. Designsystemet reduces risk but does not prove compliance, so verify the rendered result.

- **Contrast:** 4.5:1 for normal text, 3:1 for large text, and 3:1 for meaningful boundaries, controls and state indicators. Check default, hover, active, selected, focus, invalid and disabled states. Never let colour alone carry meaning.
- **Keyboard:** every interaction works, exposes the correct accessible name, role, value and state, and keeps a visible focus indicator. Test the whole path, not only the edited control: Tab and Shift+Tab follow reading order without traps, and arrow keys drive native radio groups and documented composite widgets. Never repair source order with a positive `tabindex`.
- **Screen readers:** descriptive page title and language, landmarks, heading hierarchy, grouped controls, labels, descriptions, errors, expanded and selected state, and live-region updates announced once at the right priority. ARIA supplements native HTML; it must not disguise an invalid structure.
- **Reflow and size:** content reflows at 320 CSS pixels without two-dimensional scrolling, stays usable at 200% text, and does not clip or overlap. Pointer targets meet 24 by 24 CSS pixels or the spacing exception; documented component sizes normally give more.
- **Automated checks** supplement keyboard, contrast, zoom and DOM inspection. They never replace them.

**Native semantics first.** `<a>` navigates and `<button>` acts, whatever they look like. Headings run in logical order, and visual size is a separate decision from the `h1`-`h6` level. Use real labels, legends, lists, tables and landmarks before ARIA. Keep DOM order, visual order and focus order aligned. Decorative icons get `aria-hidden`; icon-only controls need an accessible name that describes the action.

In React, use documented `asChild` composition to change the rendered element: it takes one child, which must spread received properties and forward its ref. In HTML, put the documented classes and attributes on the correct native element.

## Forms and feedback

- Group one-of-many choices with Fieldset and Radio. Use the documented `outline` variant for a larger card-like target, and keep choices vertical unless content and width support a row.
- Do not preselect a radio option without a user or domain reason. Preserve entered values and revealed fields after validation or service errors.
- In React, prefer the documented group hook. In CSS-only applications, wire shared names, descriptions, invalid state and group validation explicitly.
- Keep field errors next to their field. When several errors block progress, show an ErrorSummary in the documented location, link each message to its field and move focus as required.
- Avoid disabled actions. Prefer an active submit that explains the validation errors. Where disabled or `aria-disabled` is unavoidable, explain why and prevent the action in code, because ARIA alone does not disable behaviour.
- Loading controls use the documented loading state and `aria-busy`, while application logic prevents duplicate execution.
- Dynamic status messages use an existing live region with `role="status"` or `role="alert"`. Do not combine live-region attributes so that announcements duplicate.

## Tokens

Use semantic tokens by purpose, never by whichever value looks closest. Do not invent tokens.

**Colour.** Prefer `data-color` and semantic `--ds-color-*` over raw values. Background for page layers, Surface for raised content, Border for edges, Text for text and icons, Base for solid emphasis. `border-subtle` is decorative and cannot be the only sign that something is interactive. `data-color-scheme` resets colour variables at that boundary, so reapply `data-color` when you need both. Alert, ValidationMessage and ErrorSummary keep their severity colours and do not inherit brand colours. Components documented as neutral in the loaded version, such as Dialog or Modal and Tooltip, ignore arbitrary `data-color`; other cascading components inherit the nearest one. A token name alone does not guarantee contrast, so verify the final foreground and background pair after inheritance and state styles.

**Size and spacing.** Use `--ds-size-*` for gap, padding and margin, and let the flex or grid parent own spacing between direct children. Keep related content closer than separate sections. Define component, group and section rhythm explicitly instead of relying on browser margins, `<br>`, empty elements or one-off offsets. Hold one `sm`, `md` or `lg` size mode per context; mixing sizes must express real hierarchy, not patch a layout problem. `data-size` sets size mode for most components but sets typography or dimensions for documented exceptions such as Heading, Paragraph, Avatar and Spinner, so verify against the loaded version.

**Typography, shape and elevation.** Use Designsystemet typography classes and variables; semantic heading level and visual size are separate decisions. Use the configured `--ds-font-family`, and with Inter keep the documented weights and lowercase-l feature setting. Use radius, border-width, opacity and shadow tokens. Shadows mean elevation, not decoration, so prefer edges on dark surfaces. Keep the product's icon library; a replacement icon must preserve the meaning.

Product-specific layout values, such as an established container width, breakpoint, intrinsic media size or aspect ratio, may stay local when no token exists. Reuse the existing layout contract and explain any genuinely new value.

## Consistency

Standardise elements only when they share role, hierarchy and context.

- Header, main content and footer use the intended shared container edges, max-width and responsive side padding.
- Comparable page intros use the same wrapper, readable text width, heading treatment and internal gap.
- Repeated cards, form groups, action groups and navigation regions use the same component, variant, structure, token rhythm and responsive behaviour.
- Persistent actions and step navigation are separated from the content they complete, with a shared composition and a semantic border token.
- Mobile may be denser, but keeps the same hierarchy, semantics, states and repeated rhythm.

Before changing a repeated role, search the whole application for every comparable instance and state. Include shared consumers explicitly, or exclude them because their role differs; do not finish a one-instance fix while equivalents stay inconsistent. Apply the correction at the narrowest shared level whose consumers should all receive it, and change a shared component only after checking every consumer.

Classify every finding before changing it:

| Finding | Default action |
| --- | --- |
| Invalid or outdated Designsystemet contract | Repair it |
| Accidental drift between comparable roles | Harmonise it |
| Intentional documented or product-specific variant | Preserve it |
| Established production interaction | Preserve it; propose alternatives separately |
| Ambiguous product or interaction decision | Leave it or ask |

Read the page in visual order. Title, ingress, section heading, description, label, validation message and action each have a distinct job, so do not repeat the same instruction at every level. When Norwegian copy changes, use the `norsk-klarsprak` skill and preserve the page's målform, terminology and factual meaning.

## Workflow

Scale to the risk. A one-property correction inside an already valid component needs only runtime confirmation, the smallest fix and a focused rendered check. Use the full sequence for shared components, repeated roles, forms and validation, interaction changes or several surfaces.

1. Set the repair perimeter: affected surface, comparable roles, shared consumers, out-of-scope product decisions.
2. Resolve runtime and version: packages, scripts, theme, import order, matching documentation.
3. Inspect the baseline. With a browser or preview, check the real UI, DOM, computed styles and behaviour at relevant mobile and desktop widths. Without one, verify the rendered markup and loaded CSS selectors statically, and record which checks could not be run rather than assuming they pass.
4. Map comparable roles: component, semantic element, DOM structure, variant, colour, size, tokens, parent-owned gaps, states and responsive behaviour.
5. Classify the findings, then take the smallest valid fix from the reuse ladder. Structure before styling.
6. Implement coherently: correct directly coupled token, spacing, state and accessibility drift inside the perimeter.
7. Zoom out. Never judge the result from the edited crop. Inspect the whole page or flow at mobile and desktop widths, compare it against the shared shell and the closest equivalent surface, and ask whether it reads as one coherent product. Fix obvious in-scope drift before finishing.
8. Run a skeptic pass. Remove anything that introduces a new visual direction, duplicates a contract, broadens scope without evidence, or cannot be justified by Designsystemet.

**Done when** the runtime is understood and no undocumented API or assumed behaviour was introduced; the markup matches the loaded component contract; keyboard path, focus order, accessible names and announcements work; applicable WCAG 2.2 AA requirements pass; layout, states and every supported colour scheme hold at mobile and desktop widths; one corrected instance and one comparable reference were checked side by side and shared consumers remain correct; the whole page reads as one system; and no unnecessary custom value, duplicate class or override was added. Any rendered or interactive check you could not run stays explicit, never reported as verified.

## Sources

- [Components](https://designsystemet.no/no/components)
- [Fundamentals](https://designsystemet.no/no/fundamentals)
- [Design tokens](https://designsystemet.no/no/fundamentals/theme/variables)
- [Best practices](https://designsystemet.no/no/best-practices)
- [Patterns](https://designsystemet.no/no/patterns)
- [Storybook](https://storybook.designsystemet.no)
- [GitHub source and migration history](https://github.com/digdir/designsystemet)

Prefer the application's pinned version over the latest examples. When a documented contract is uncertain, use the official source, the component CSS and types, and the migration notes.
