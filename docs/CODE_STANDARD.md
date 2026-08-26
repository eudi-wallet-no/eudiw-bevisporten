# Code Standard

## Logical grouping

- Separate logical phases with one blank line, also in short methods.
- Keep statements in the same phase together. Do not add a blank line after every statement.
- Code: group fetch or derive, validate or guard, execute, persist and return as distinct phases.
- Tests: group arrange, act and assert as distinct phases.

## Obvious code

- Prefer explicit branches and named methods over compact code that makes the reader reconstruct control flow.
- When a feature toggle selects between contracts, branch on the toggle first and call the implementation for that contract.
- Do not construct API versions, endpoint paths or response handling indirectly from a boolean.

## Comments

Code says what it does. Comment only the reason it does it that way: a deliberate choice, an external constraint, a known gap. If a comment restates the code, delete it.

## API documentation

If the application has Swagger, document REST endpoints: `@Operation` on the endpoint, `@Schema` on the response type. Describe meaning, not names.
