# Progress Tracker

Update this file after every meaningful implementation change.

## Current Phase

Sprint 2: The Invite Engine, Security Walls & UI Scaffold

## Current Goal

Sprint 2 backend complete. Next: Android Jetpack Compose UI scaffold (Part B).

## Completed

### Part A: Project Scaffolding & Environment
- [x] Project scaffolding with Spring Boot and Gradle
- [x] Layered architecture structure (config, features/auth/routing, features/auth/service, features/auth/repository, features/auth/models)
- [x] MySQL database connection with HikariCP
- [x] Flyway migration for initial tables (V1: users, families, family_memberships; V2: password_resets)
- [x] JPA entity models (User, Family, FamilyMembership, PasswordReset)
- [x] Repository layer (UserRepository, FamilyRepository, FamilyMembershipRepository, PasswordResetRepository)
- [x] Controller layer with health check endpoint
- [x] Git repository initialization with .gitignore
- [x] Gradle wrapper generation
- [x] Application builds successfully
- [x] MySQL user creation and database setup
- [x] Application starts successfully
- [x] Health endpoint returns 200 OK
- [x] Kotlin JPA plugin configuration (no-arg constructors, all-open)

### Part B: Core Business Logic
- [x] Request/Response DTOs with Bean Validation (RegisterRequest, LoginRequest, AuthResponse)
- [x] Custom exceptions (EmailAlreadyExistsException, InvalidCredentialsException)
- [x] GlobalExceptionHandler with proper HTTP status codes (400, 401, 409, 500)
- [x] AuthService.registerUser() with BCrypt hashing (cost factor 12)
- [x] AuthService.loginUser() with BCrypt verification
- [x] Invite code generation (NP-XXXXXX format, 6 characters)
- [x] Automatic "Krousa Me" family creation on registration
- [x] Email uniqueness validation
- [x] Transactional boundary for registration (atomic user+family+membership creation)
- [x] AuthController POST /register endpoint (201 Created)
- [x] AuthController POST /login endpoint (200 OK)
- [x] EmailService with Resend Java SDK
- [x] Resend API key configuration in .env
- [x] End-to-end email delivery test

### Part C: Documentation
- [x] UML diagrams (Use Case, Activity, Class, Sequence)
- [x] Database schema documentation
- [x] README.md with API documentation
- [x] Context methodology documentation (6 files in docs/context/)

## In Progress

None - Sprint 1 is complete.

## Next Up

Sprint 2 Part B — Android Jetpack Compose UI (from sprint2-invite-engine-security-ui.md):
- Compose project scaffolding + package structure
- Network client (Ktor/Retrofit) configuration
- AuthUiState sealed interface + ViewModel
- Login, Register, ForgotPassword, ResetPassword screens
- Dashboard scaffold with workspace switcher and invite modal

## Sprint 2 — Completed

### Phase 1: Password Reset Completion
- [x] `ForgotPasswordRequest` DTO
- [x] `ResetPasswordRequest` DTO
- [x] `InvalidTokenException` (400)
- [x] `GlobalExceptionHandler` updated with InvalidTokenException handler
- [x] `EmailService` annotated with `@Service`
- [x] `AuthService.requestPasswordReset()` — token generation + email dispatch
- [x] `AuthService.resetPassword()` — token validation + password update + token cleanup
- [x] `POST /api/v1/auth/forgot-password` endpoint
- [x] `POST /api/v1/auth/reset-password` endpoint
- [x] End-to-end test verified (email received, password reset successful)

### Phase 2: Invite Engine
- [x] `ResourceNotFoundException` (404)
- [x] `MembershipAlreadyExistsException` (409)
- [x] `GlobalExceptionHandler` updated with both new handlers
- [x] `JoinFamilyResponse` DTO
- [x] `FamilyService.joinFamilyByCode()` — lookup by code, duplicate guard, membership insert
- [x] `GET /api/v1/family/join?code=...` endpoint (X-User-Id header for dev)

### Phase 3: Membership Security Filter
- [x] `FamilyMembershipInterceptor` — reads {familyId} path variable + X-User-Id, blocks non-members with 403
- [x] `WebConfig` — interceptor registered on `/api/v1/family/**`
- [x] `GET /api/v1/family/{familyId}/feed` placeholder endpoint for testing the interceptor

## Open Questions

- Should we implement JWT token-based authentication or session-based? (Decision needed for future sprints)
- What is the maximum number of family members per family? (Currently unlimited)
- Should we implement email verification for registration? (Not in Sprint 1 scope)
- What should happen when a user tries to register with an existing email? (Currently returns 409)

## Architecture Decisions

- **Frontend is a separate repository**: The Android Jetpack Compose app lives in its own repo and connects to this backend exclusively via REST API endpoints. No frontend code belongs in `nakpom-backend`.

- **BCrypt with cost factor 12**: Chosen for password hashing to balance security and performance. 2¹² = 4096 iterations.
- **Invite code format NP-XXXXXX**: 6-character uppercase alphanumeric code provides over 2 billion combinations.
- **Transactional boundary for registration**: User, family, and membership creation wrapped in single transaction to ensure atomicity.
- **MySQL user 'nakpom'**: Created dedicated database user instead of using root for security (root uses auth_socket).
- **Layered architecture**: Clear separation between routing, service, repository, and model layers for maintainability.

## Session Notes

- MySQL root user uses auth_socket authentication, not password authentication. Created dedicated 'nakpom' user with secure password (configured in .env).
- Application runs on port 8080. Health endpoint at GET /api/v1/health.
- Database name: nakpom_db. Tables: users, families, family_memberships, password_resets.
- Foreign key constraints are in place with CASCADE delete.
- Flyway migrations V1__Create_initial_tables.sql and V2__Create_password_resets.sql applied successfully.
- Gradle wrapper version 8.5. Spring Boot version 3.2.5.
- Resend API configured for transactional email delivery (password reset flow).
