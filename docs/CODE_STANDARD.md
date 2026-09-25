# Code Standard

Keep code clear, predictable and consistent with the existing codebase. The default is simple and unsurprising.

## Core principles

- Follow established architecture and project patterns. Extend existing flows instead of adding abstractions, parallel flows or dependencies unless there is a clear reason.
- Keep each function focused, short and easy to read: guard, fetch, validate, transform, execute, return. Avoid deep nesting and hidden control flow.
- Name code by intent and domain language. Boolean names should read like predicates: `is`, `has`, `can` or `should`. Extract helpers only when their names clarify intent.
- Keep business rules separate from transport, persistence and UI concerns. Prefer returned values over mutation unless the function is an explicit state transition.
- Prefer explicit behavior over clever shortcuts. Validate early, fail clearly and do not hide invalid state, silent fallbacks or fake success.
- Preserve APIs, data shapes and existing behavior unless the task explicitly changes them.

## Change discipline

- Keep changes small and reviewable. When in doubt, favor the surrounding codebase and the least surprising design.
- Prefer the narrowest correct change. Avoid adding new abstractions or moving responsibilities without a concrete benefit.
- Add documentation for public behavior and API contracts. Use comments only for intent, constraints or known trade-offs.

## Testing expectations

- Write tests for observable behavior and important boundaries, not implementation details.
- When testing Spring `RestClient` integrations, use `MockRestServiceServer` bound to the same `RestClient.Builder` before building the client.
- Add `@DisplayName` to every test class and test method. Use Gherkin-style descriptions in the form
  `When ..., then ... is expected` or `When ... with ..., then ... is expected`.
- Use `private static final` uppercase constants for shared immutable test data. Keep one-use strings local to the method where they are used.

## Code quality

- Prefer `String.formatted(...)` and other clear formatting methods over string concatenation with `+`.
- Surface and handle errors explicitly. Do not hide errors, suppress failure states or return false success.
- Keep business logic testable and deterministic. Prefer values and explicit returns over hidden side effects whenever practical.