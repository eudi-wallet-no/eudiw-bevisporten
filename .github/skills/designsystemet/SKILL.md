---
name: designsystem
description: "All UI follows Digitaliseringsdirektoratet's Designsystemet. Reuse its components and design tokens instead of writing custom UI. Use when building or changing anything visual."
license: Digitaliseringsdirektoratet
allowed-tools: ['view', 'grep', 'bash']
---

# Designsystemet

All UI follows [Designsystemet](https://designsystemet.no). It is the default, not a suggestion.

## Rule

Reuse before you build:

1. Use a Designsystemet component
2. Compose with design tokens
3. Build custom only when nothing above fits — explain why, and still use tokens

Never hard-code colours, spacing, type, radius or shadow. Values come from tokens.

## Where to look

- [Components](https://designsystemet.no/no/components) — what exists and how to use it
- [Design tokens](https://designsystemet.no/no/fundamentals/design-tokens) — the values you are allowed to use
- [Storybook](https://storybook.designsystemet.no) — live examples

## Verify, don't guess

Designsystemet is mostly CSS classes rather than web components. Check the documentation before using a class or token name. If you cannot find it, it does not exist.

Some templates still contain outdated patterns. Verify against the documentation before copying.
