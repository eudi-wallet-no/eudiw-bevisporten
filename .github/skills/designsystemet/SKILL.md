---
name: designsystem
description: "Build and change UI with Digitaliseringsdirektoratet's Designsystemet. Verify component contracts, compare related surfaces, standardise shared roles and reuse components, compositions and tokens before writing custom UI. Use for anything visual."
license: Digitaliseringsdirektoratet
allowed-tools: ['view', 'grep', 'bash']
---

# Designsystemet

All UI follows [Designsystemet](https://designsystemet.no). It is the default, not a suggestion.

## Mission

Build and change UI as part of an existing system, not as isolated markup. A component can use Designsystemet classes and still be wrong when it uses the wrong variant, breaks the documented DOM contract, or drifts from an established composition elsewhere in the same flow. Making each element look acceptable is not enough: the same role, hierarchy and context must follow the same Designsystemet component and composition contract across comparable surfaces.

## Source of truth

Decide what is correct in this order:

1. The documentation and Storybook for the Designsystemet version the application actually loads.
2. Shared Designsystemet-based components, fragments and composition classes in the application.
3. Established patterns with the same role, hierarchy and context in the same flow.
4. The closest comparable UI in the repository.

Do not copy the first occurrence you find. Templates can contain outdated or incomplete patterns. Map comparable occurrences first and distinguish documented variants from accidental drift.

- [Components](https://designsystemet.no/no/components) — what exists and how to use it
- [Design tokens](https://designsystemet.no/no/fundamentals/design-tokens) — the values you are allowed to use
- [Storybook](https://storybook.designsystemet.no) — live examples

## Reuse ladder

Reuse before you build:

1. Use a Designsystemet component with its documented markup, properties and variants.
2. Reuse an existing local composition of Designsystemet components, such as a page intro, form section or action group.
3. Compose Designsystemet components with design tokens.
4. Build custom only when nothing above fits. Explain why, keep it compositional, and still use tokens.

Never hard-code colours, spacing, type, radius or shadow — values come from tokens. Do not create a parallel class when an existing wrapper or shared composition already represents the same role.

## Component contracts

Designsystemet is primarily CSS classes and attributes rather than web components, so correctness depends on the complete HTML contract. Using `class="ds-card"` alone does not prove that a card follows Designsystemet: its variant, colour, interactive element, content hierarchy and DOM structure must match the component contract too.

- Verify class names, `data-*` values and supported variants against the loaded version. For example, `data-color` and `data-variant` are different APIs and are not interchangeable.
- Preserve the documented element type and child structure. Component selectors may depend on direct children; an unnecessary wrapper can silently disable spacing, interactive heading and state styles.
- Use semantic HTML first: real links for navigation, buttons for actions, headings in order, labels for controls and `nav` landmarks for navigation groups.
- For a one-of-many choice, prefer the documented `Fieldset` and radio contract over custom selectable cards. Use the `outline` field variant when the choices need a larger, card-like target, and keep the options vertical unless the content and available width clearly support a row.
- Do not preselect a radio choice without a user or domain reason. If the choice reveals different fields, show only the relevant field group and preserve the choice after validation or service errors.
- Preserve built-in hover, focus, active, selected, disabled and validation states. Do not replace them with local imitations.
- Inspect the generated CSS or installed package when documentation and the running application appear to disagree. Never guess a token, variant or selector.

## Consistency contract

Standardise shared roles before fine-tuning individual elements. Consistency does not mean making everything identical: different roles or documented variants may use different contracts, but differences must be explained by role or context, not by which template happened to be edited.

- A page intro made of title and ingress uses the same established wrapper, text width and internal gap on comparable pages. Treat it as its own typographic hierarchy: use supported `data-size` values so the title is clearly more prominent than the ingress, and the ingress clearly distinguishable from ordinary body content. Heading level alone does not create that visual hierarchy.
- Header, main content and footer share the intended container edges, max-width and responsive side padding. Headings, body text, cards, lists and actions in the same content track align to the same edges.
- Repeated cards with the same purpose use the same Designsystemet variant, heading level, content structure, padding behaviour and responsive layout.
- Related content has a smaller internal gap than the distance to the next section. Use the established token scale for both.
- Separate persistent action or step-navigation areas from the content they complete. Prefer a shared composition with token-based spacing and a Designsystemet border token over ad hoc `<hr>` elements or page-specific margins.
- Give pages with persistent step navigation an explicit shared structure where the content region and navigation are siblings. Do not let a form's smaller internal field gap determine the distance to the navigation; use the button's `form` attribute and place shared interactive state on a common wrapper when the action must remain outside the form.
- Mobile layouts may be denser, but must preserve the same hierarchy, component states and repeated rhythm.

## Spacing and gap contract

Spacing is part of the component contract, not finishing polish. Whenever a visual composition changes, inspect the full affected composition and align spacing with Designsystemet and the established token scale.

- Let the parent composition own the rhythm between direct children. Prefer `flex` or `grid` with a token-based `gap` over child margins, line breaks, empty elements or one-off offsets.
- Use Designsystemet size tokens for every gap. Reuse an existing composition with the correct gap before introducing a new class or spacing rule.
- Keep related content closer together than separate sections. Define intentional internal, component and section gaps instead of relying on browser defaults or incidental component margins.
- Inspect title, ingress, body, links, actions and neighbouring sections together. If any part of the affected composition falls outside the gap system, correct it in the same change.
- Apply the correction at the narrowest shared level whose consumers should all receive it. Do not turn a local spacing fix into an unrelated repository-wide cleanup.

## Content coherence

Treat content as part of the Designsystemet composition, not as isolated sentences:

- Before writing, read the whole affected page in its visual order. Identify what the title, ingress, section heading, description, label, validation message and action already communicate.
- Give each text element a distinct job in the user journey. Do not repeat the same instruction in the page intro, component description and section heading merely because each component supports text.
- Make new copy fit the surrounding hierarchy and the next action. Adjust nearby text when that is necessary for a coherent progression, but do not use a local text change as a reason to rewrite the whole page.
- After editing, read the rendered page from top to bottom. Remove repetition, resolve terminology drift and verify that the content forms a clear progression from purpose to choice, required information and action.

## Workflow

1. **Inspect the actual UI.** Open the affected surface at relevant desktop and mobile widths. Read computed styles or component states when visual differences are subtle.
2. **Map comparable roles.** Find all nearby occurrences with the same role and note their wrapper, Designsystemet component, variant, token use, DOM structure, parent-owned gaps and states.
3. **Choose the standard.** Apply the source-of-truth order above. Prefer a documented component or an already correct shared composition over a new local solution.
4. **Implement at the smallest level.** Reuse the established wrapper or fragment first, then supported variants and tokens, and only then a local override. Do not add CSS to compensate for incorrect component markup.
5. **Verify visually and functionally.** Compare the changed surface side by side with at least one already correct reference. Check relevant viewports, keyboard focus, hover/active states, wrapping, alignment and content hierarchy. Measure or inspect computed gaps when the rhythm is subtle. Read the page from top to bottom and confirm the spacing and gap contracts hold, including a clear but restrained boundary before action regions.
6. **Run a skeptic pass.** Remove changes that introduce a new visual direction, duplicate an existing contract, expand scope without evidence, or cannot be justified by Designsystemet or an established composition.

## Guardrails

- Preserve information architecture, content, component roles and product behaviour unless the task explicitly changes them.
- Keep diffs small and explainable. Do not combine Designsystemet alignment with unrelated refactoring.
- Change a shared component only when all of its consumers should receive the same correction; otherwise use the narrowest established composition.
- Preserve or improve contrast, focus visibility, readable line length and touch target size.
- Stop when the next change requires taste, a new design direction or a product decision rather than Designsystemet alignment.

## Completion standard

A visual task is complete only when:

- the component markup and variants are valid for the loaded Designsystemet version;
- comparable roles in scope use the same justified composition;
- the affected composition uses parent-owned, token-based gaps with no accidental margin-driven rhythm;
- no unnecessary custom values or duplicate classes were introduced;
- the whole affected composition and its closest comparable surfaces were inspected, not only the edited element — a mismatch such as one page intro lacking the shared intro wrapper belongs to the same scoped work;
- the rendered result was compared with an established reference in relevant viewports and states.
