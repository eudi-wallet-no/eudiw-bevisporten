# Code Standard

Keep code clear, predictable and consistent with the existing codebase. The default is simple and unsurprising.

- Follow established architecture and project patterns. Extend existing flows instead of adding abstractions, parallel flows or dependencies unless there is a clear reason.
- Keep each function focused, short and easy to read: guard, fetch, validate, transform, execute, return. Avoid deep nesting and hidden control flow.
- Name code by intent and domain language. Boolean names should read like predicates: `is`, `has`, `can` or `should`. Extract helpers only when their names clarify intent.
- Keep business rules separate from transport and persistence. Prefer returned values over mutation unless the function is a clear state transition.
- Prefer explicit behavior over clever shortcuts. Validate early, fail clearly and do not hide invalid state or return fake success.
- Preserve APIs, data shapes and existing behavior unless the task explicitly changes them.
- Write tests for observable behavior and important boundaries, not implementation details.
- Add `@DisplayName` to every test class and test method. Use Gherkin-style descriptions in the form
  `When ..., then ... is expected` or `When ... with ..., then ... is expected`.
- Use `private static final` uppercase constants for shared immutable test data. Keep one-use strings local to the method where they are used.
- Prefer `String.formatted(...)` and other clear formatting methods over string concatenation with `+`.
- Add documentation for public behavior and API contracts. Use comments only for intent, constraints or known trade-offs.
- Keep changes small and reviewable. When in doubt, favor the surrounding codebase and the least surprising design.