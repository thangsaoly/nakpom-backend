# Architecture Context

## Stack

| Layer     | Technology                  | Role   |
| --------- | --------------------------- | ------ |
| Backend   | Spring Boot 3.2.5 + Kotlin | REST API framework |
| Database  | MySQL 8.4 + HikariCP       | Data persistence and connection pooling |
| ORM       | Spring Data JPA + Hibernate | Object-relational mapping |
| Migration | Flyway                     | Database schema versioning |
| Build     | Gradle 8.5 (Kotlin DSL)    | Build automation and dependency management |
| Frontend  | Android + Jetpack Compose  | Mobile application (future) |
| Security  | BCrypt                     | Password hashing |

## System Boundaries

- `src/main/kotlin/com/nakpom/config/` — Application configuration (database, security, environment settings)
- `src/main/kotlin/com/nakpom/features/auth/routing/` — HTTP controllers and request/response handling
- `src/main/kotlin/com/nakpom/features/auth/service/` — Business logic and transaction management
- `src/main/kotlin/com/nakpom/features/auth/repository/` — Data access layer (Spring Data JPA repositories)
- `src/main/kotlin/com/nakpom/features/auth/models/` — JPA entities and DTOs
- `src/main/kotlin/com/nakpom/exception/` — Global exception handling
- `src/main/resources/db/migration/` — Flyway database migration scripts
- `src/main/resources/application.yml` — Application configuration

## Configuration Model

- Local secrets and runtime values live in the project-root `.env` file.
- `application.yml` imports `.env` with `spring.config.import`, so application code must read those values through Spring's property environment, for example constructor parameters annotated with `@Value` or typed `@ConfigurationProperties`.
- Do not read project `.env` values with `System.getenv()` inside Spring services. Spring-imported `.env` entries are not added to the OS environment, which can cause bean construction failures at startup.

## Storage Model

- **MySQL Database**: Stores all persistent data including users, families, family memberships, and relationships. Uses InnoDB engine for transaction support and foreign key constraints.
- **No file storage**: Current scope does not include file uploads or blob storage.

## Auth and Access Model

- **Authentication**: Users authenticate via email/password combination. Passwords are hashed using BCrypt with cost factor 12 before storage.
- **Ownership**: Each family has a unique owner (the user who created it). Ownership is tracked via the `role` field in `family_memberships` table.
- **Access Control**: Family membership is controlled via invite codes. Only users with valid invite codes can join a family. Family owners can manage family settings (future feature).

## Invariants

1. **Transaction Boundary**: All database writes within a single business operation (e.g., registration) must be wrapped in a single `@Transactional` boundary to ensure atomicity.
2. **Password Security**: Plaintext passwords must never be stored in the database. All passwords must be hashed with BCrypt before persistence.
3. **Email Uniqueness**: Email addresses must be unique across all users. Registration must fail if email already exists.
4. **Invite Code Uniqueness**: Family invite codes must be globally unique. Code generation must retry on collision.
5. **Foreign Key Integrity**: Family memberships must reference valid users and families. Cascade delete ensures cleanup when users or families are removed.
6. **No Business Logic in Controllers**: Controllers must only handle HTTP concerns (validation, status codes). All business logic belongs in the service layer.
7. **Repository Interfaces Only**: Repository layer must use Spring Data JPA interface definitions. No custom SQL implementation unless absolutely necessary.
8. **DTO Separation**: API request/response DTOs must be separate from JPA entities to prevent accidental exposure of sensitive data (e.g., password hashes).
