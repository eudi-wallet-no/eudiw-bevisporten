# Code Standard

## Logical grouping

- Tests: arrange, act, assert.
- Code: fetch, validate, execute, persist. Return early.

## Comments

Code says what it does. Comment only the reason it does it that way: a deliberate choice, an external constraint, a known gap. If a comment restates the code, delete it.

## API documentation

If the application has Swagger, document REST endpoints: `@Operation` on the endpoint, `@Schema` on the response type. Describe meaning, not names.

