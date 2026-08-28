# Code Standard

Code should read as a short, unsurprising sequence of steps. Optimize for the reader, not the writer.

## Flow

- Order work naturally: guard, fetch, validate, transform, execute or persist, return. Omit phases that do not apply.
- Use guard clauses for invalid or terminal cases. Avoid deep nesting.
- Separate logical phases with one blank line, also in short methods. Keep statements in the same phase together.
- Prefer explicit branches and named methods over compact code that makes the reader reconstruct control flow.
- When a feature toggle selects between contracts, branch on the toggle first. Do not derive API versions, paths or response handling from a boolean.
- Extend an existing flow or endpoint before adding a parallel one. A new path needs a reason the existing one cannot serve.

## Functions and data

- Give each function one responsibility: perform one step or orchestrate clearly named steps.
- Extract a helper only when its name makes the workflow easier to read.
- Prefer returned values over mutating inputs.
- Keep decisions and transformations separate from I/O and state changes.

## Names

- Name code by business intent. Use precise verbs and the project's domain language.
- Name booleans as predicates: `is`, `has`, `can` or `should`.
- Name a complex condition when the name removes mental work. Keep simple conditions inline.
- Name a method that returns a transformed copy as a conversion (`toX`, `asX`), not a mutation.

## Boundaries

- Controllers adapt requests and responses. Services coordinate use cases. Persistence reads and writes data.
- Keep business rules out of transport and persistence code.

## Errors

- Validate early, where enough context exists to report the actual problem.
- Fail explicitly. Do not ignore errors, hide invalid state or return success-shaped fallbacks.
- Catch an error only to recover or add useful context.

## Tests

- Separate arrange, act and assert with one blank line.
- Test observable behavior and important boundaries, not implementation shape.

## Comments

Code says what it does. Comment only why: a deliberate choice, an external constraint or a known gap. Delete comments that restate the code.

## API documentation

If the application has Swagger, document REST endpoints: `@Operation` on the endpoint, `@Schema` on the response type. Describe meaning, not names.
