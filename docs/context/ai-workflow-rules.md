# AI Workflow Rules

## Approach

Build the NakPom project incrementally using a spec-driven workflow. Context files define what to build, how to build it, and the current state of progress. Always implement against these specs — do not infer or invent behavior from scratch. The project follows the Sprint 1(There will be Sprint 2, 3, etc.) plan from the official project plan PDF.

## Scoping Rules

- Work on one feature unit at a time (e.g., complete registration before starting login)
- Prefer small, verifiable increments over large speculative changes
- Do not combine unrelated system boundaries in a single implementation step
- Test each increment before moving to the next
- Follow the layered architecture: models → repositories → services → controllers

## When to Split Work

Split an implementation step if it combines:

- Multiple database schema changes (split into separate Flyway migrations)
- Controller changes and service layer business logic
- Multiple unrelated API routes
- Authentication logic and business logic
- Behavior not clearly defined in the context files

If a change cannot be verified end to end quickly, the scope is too broad — split it.

## Handling Missing Requirements

- Do not invent product behavior not defined in the context files
- If a requirement is ambiguous, resolve it in the relevant context file(or Sprint plan PDF) before implementing
- If a requirement is missing, add it as an open question in `progress-tracker.md` before continuing
- Refer to the official project plan PDF for Sprint 1(or future sprints) requirements
- Check the UML diagrams for design specifications

## Protected Files

Do not modify the following unless explicitly instructed:

- `src/main/resources/db/migration/V1__*` — Once deployed, never modify existing migrations
- Any third-party library dependencies in `build.gradle.kts` without justification
- The official project plan PDF files

## Keeping Docs in Sync

Update the relevant context file whenever implementation changes:

- System architecture or boundaries → Update `architecture.md`
- Storage model decisions → Update `database-schema.md`
- Code conventions or standards → Update `code-standards.md`
- Feature scope → Update `project-overview.md`
- Progress → Update `progress-tracker.md`
- API changes → Update `api.md` (when created)

## Before Moving to the Next Unit

1. The current unit works end to end within its defined scope
2. No invariant defined in `architecture.md` was violated
3. `progress-tracker.md` reflects the completed work
4. `./gradlew clean build` passes
5. Database migrations run successfully
6. API endpoints return expected responses (test with curl or Postman)
7. Code follows all standards in `code-standards.md`

## Testing Requirements

- Write unit tests for service layer business logic
- Test repository layer with in-memory database (H2) if possible
- Test API endpoints with integration tests
- Verify database constraints with test data
- Test error paths (invalid input, duplicate emails, wrong passwords)

## Security Checklist

Before marking a feature complete, verify:

- [ ] Passwords are hashed with BCrypt
- [ ] Input validation is enforced with `@Valid`
- [ ] SQL injection protection (JPA handles this)
- [ ] No sensitive data in logs or error responses
- [ ] Foreign key constraints are in place
- [ ] Authentication failures return generic error messages
