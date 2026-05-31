# Code Standards

## General

- Keep functions small and single-purpose
- Fix root causes, do not layer workarounds
- Do not mix unrelated concerns in one class or function
- Prefer composition over inheritance
- Write code that is easy to delete, not easy to extend

## Kotlin

- Use Kotlin idiomatic patterns (data classes, extension functions, null safety)
- Avoid nullable types where possible, use non-null by default
- Use `val` for immutable references, `var` only when mutability is required
- Prefer expression bodies for simple functions
- Use sealed classes for restricted class hierarchies
- Validate unknown external input at system boundaries before trusting it

## Spring Boot

- Use constructor injection for dependencies (no field injection)
- Annotate service methods with `@Transactional` for database operations
- Use `@Valid` for request validation in controllers
- Return `ResponseEntity<T>` for typed HTTP responses
- Use `@RestControllerAdvice` for centralized exception handling
- Keep controllers thin - delegate all business logic to services
- Use Spring Data JPA derived query methods when possible

## Database

- Use Flyway for all schema changes - never modify database manually
- Name migrations with descriptive prefixes: `V{version}__{description}.sql`
- Use foreign key constraints to enforce referential integrity
- Add indexes on frequently queried columns (email, invite_code)
- Use `ddl-auto: validate` in production to prevent accidental schema changes
- Use `ddl-auto: update` only in development for rapid prototyping

## API Design

- Follow RESTful conventions for resource naming
- Use appropriate HTTP status codes (200, 201, 400, 401, 409, 500)
- Return consistent JSON response shapes
- Include timestamp in all API responses
- Use plural nouns for collection endpoints (`/users`, `/families`)
- Use kebab-case for URL paths (`/api/v1/auth/register`)
- Use camelCase for JSON property names

## Security

- Never log or return password hashes in API responses
- Use BCrypt with minimum cost factor 12 for password hashing
- Validate all input at the controller layer with `@Valid`
- Use parameterized queries to prevent SQL injection (handled by JPA)
- Do not expose internal error details to clients
- Use generic error messages for authentication failures to prevent user enumeration

## Error Handling

- Create custom exceptions for business logic errors
- Use `@RestControllerAdvice` for centralized exception handling
- Return structured error responses with field-level validation details
- Log full stack traces server-side, return minimal info to clients
- Use appropriate HTTP status codes for different error types

## File Organization

- `src/main/kotlin/com/nakpom/config/` — Configuration classes
- `src/main/kotlin/com/nakpom/features/{feature}/routing/` — Controllers
- `src/main/kotlin/com/nakpom/features/{feature}/service/` — Business logic
- `src/main/kotlin/com/nakpom/features/{feature}/repository/` — Data access
- `src/main/kotlin/com/nakpom/features/{feature}/models/` — Entities and DTOs
- `src/main/kotlin/com/nakpom/exception/` — Exception handlers
- `src/main/resources/db/migration/` — Database migrations
- `src/main/resources/application.yml` — Configuration

## Naming Conventions

- Classes: PascalCase (`UserService`, `AuthController`)
- Functions/Methods: camelCase (`registerUser`, `findByEmail`)
- Constants: UPPER_SNAKE_CASE (`MAX_PASSWORD_LENGTH`, `DEFAULT_ROLE`)
- Database tables: snake_case (`users`, `family_memberships`)
- Database columns: snake_case (`user_id`, `invite_code`)
- API endpoints: kebab-case (`/api/v1/auth/register`)
- Package names: lowercase (`com.nakpom.features.auth`)

## Comments and Documentation

- Add KDoc comments for public APIs
- Comment complex business logic with "why" not "what"
- Keep comments up to date with code changes
- Avoid obvious comments (`// increment i`)
- Document non-obvious design decisions in code comments
