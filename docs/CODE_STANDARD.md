# Code Standard

Keep code clear, predictable and consistent with the existing codebase. The default is simple and unsurprising.

- Follow the established architecture and project patterns before introducing a new one. Extend existing flows unless there is a clear reason not to.
- Keep the main flow easy to read: guard, fetch, validate, transform, execute, return. Avoid deep nesting and hidden control flow.
- Give each function one responsibility. Keep business rules out of transport and persistence code.
- Name code by intent and domain language. Boolean names should read like predicates: `is`, `has`, `can`, `should`.
- Prefer explicit behavior over clever shortcuts. Validate early, fail clearly and do not hide invalid state or return fake success.
- Keep contracts stable. Preserve APIs, data shapes and existing behavior unless the task explicitly changes them.
- Prefer returned values over mutation when the function is not clearly a state transition. Keep data mutations and business logic explicit.
- Extract helpers only when the name makes the logic easier to follow; do not split code just to be abstract.
- Write tests for observable behavior and important boundaries, not implementation details.
- Add `@DisplayName` to every test class and test method. Use Gherkin-style descriptions in the form
  `When ..., then ... is expected` or `When ... with ..., then ... is expected`.
- Add documentation for public behavior and API contracts. Use comments only for intent, constraints or known trade-offs.
- Keep changes small and reviewable. Avoid unnecessary abstractions, parallel flows and new dependencies when a simpler fit exists.
- When in doubt, favor the surrounding codebase and the least surprising design over a locally clever solution.
