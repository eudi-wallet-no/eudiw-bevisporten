# Designsystemet in this repository

The frontend uses Thymeleaf. Keep each app's existing UI system. Do not add npm, a
frontend build, a second theme or Designsystemet to an app that does not use it.

## App setup

- `eudiw-issuer-ui`: local base CSS, local `digital-lommebok.css` theme (`v1.0.8`)
  and Inter. The base CSS version is not recorded in the repository.
- `eudiw-issuer-ui-demo`: local `digdir.css`, versioned CDN base CSS and local
  `styles.css`. Both Designsystemet files use `v1.20.0`. Theme configuration:
  `apps/eudiw-issuer-ui-demo/src/main/resources/static/css/designsystemet.config.json`.
- `eudiw-verifier-demo` and `eudiw-idporten-login`: keep their existing CSS. Do not
  add Designsystemet as part of an ordinary change.

The first two apps load Designsystemet assets before local `styles.css`. Use local
classes for layout and product styling. Use semantic `--ds-*` tokens for colors,
spacing and radii.

## Layout contract

Aim for calm public-service pages inspired by [digdir.no](https://www.digdir.no/) and
[uutilsynet.no](https://www.uutilsynet.no/). Use the direction, not a pixel copy:

- Keep header and footer backgrounds full width. Use the app's established centered
  wrapper for their content. Do not add a second page-width wrapper.
- Reuse the app's existing `<main>` structure. Use one page width, narrower text
  measures for prose and wider areas for grids.
- Let containers own section spacing. Let components own internal spacing. Use `gap`,
  padding and `--ds-size-*` tokens instead of one-off margins.
- Use the neutral page background and semantic surface tokens for grouped content.
  Scope `data-color-scheme` to the complete surface that needs it.
- Start with one column. Expand rows or grids only when content fits. Avoid fixed page
  dimensions that prevent reflow or cause clipping. Test long labels, mobile and zoom.
- Keep cards for choices and related content, not for every section.

Add custom motion only for non-essential state changes. Respect
`prefers-reduced-motion: reduce`. CSS does not provide keyboard or focus logic.

## UI rules

- Use documented `ds-*` components and attributes.
- Use `--ds-*` tokens for values; use local classes for layout.
- Do not mix base CSS or themes from different Designsystemet versions.
- Do not edit vendored or generated Designsystemet CSS by hand during routine UI work.
- `data-size` and `data-color` can affect descendants. Set them on the smallest intended element.
- Use `<button>` for actions and `<a href>` for navigation. Preserve DOM order and control types.

## Documentation and updates

Use documentation that matches the loaded version. Read the component's
[overview](https://designsystemet.no/no/components/docs/button/overview), `code` and
`accessibility` pages, plus [tokens](https://designsystemet.no/no/fundamentals/theme/variables)
and [patterns](https://designsystemet.no/no/patterns).

Check names, labels, heading order, keyboard operation, visible focus, contrast and
relevant states on mobile and desktop.

Updating Designsystemet is deliberate work. Update base CSS, theme CSS and tracked theme
configuration together, update this app list and verify light and dark surfaces. If the
source or generation command is not in the repository, do not guess it.
