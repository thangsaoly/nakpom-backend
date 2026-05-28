# Sprint 1: Identity & The "Krousa Me" Automator (Week 1)

This plan covers the complete Sprint 1 scope: project scaffolding, environment setup, and the core registration/login business logic with automatic "Krousa Me" family creation.

## Technology Stack
- **Backend**: Spring Boot 3.x with Kotlin
- **Build Tool**: Gradle (Kotlin DSL)
- **Database**: MySQL (local installation)
- **Connection Pool**: HikariCP (included in Spring Boot)
- **ORM**: Spring Data JPA with Hibernate
- **Migration**: Flyway (for schema versioning)
- **Password Hashing**: BCrypt (org.mindrot:jbcrypt)

---

## Part A: Project Scaffolding & Environment

### Step 1: Initialize Spring Boot Project

#### 1.1 Create Project Structure
Create project manually with Gradle Kotlin DSL:
- **Project**: Gradle - Kotlin
- **Language**: Kotlin
- **Spring Boot**: 3.2.5
- **Dependencies**:
  - Spring Web
  - Spring Data JPA
  - MySQL Driver
  - Flyway
  - Validation
  - HikariCP (included with Spring Boot)
  - jbcrypt (BCrypt password hashing)
- **Note**: Generate Gradle wrapper after creating build.gradle.kts

#### 1.2 Create Layered Architecture Directory Structure
```
src/main/kotlin/com/nakpom/
├── NakPomApplication.kt              # Main entry point
├── config/
│   ├── DatabaseConfig.kt             # Database configuration
│   └── SecurityConfig.kt             # Security configuration (placeholder)
├── features/
│   └── auth/
│       ├── routing/
│       │   ├── AuthController.kt     # Auth endpoints (register, login)
│       │   └── HealthController.kt   # Health check endpoint
│       ├── service/
│       │   └── AuthService.kt        # Business logic (register, login, Krousa Me hook)
│       ├── repository/
│       │   ├── UserRepository.kt
│       │   ├── FamilyRepository.kt
│       │   ├── FamilyMembershipRepository.kt
│       │   └── PasswordResetRepository.kt
│       └── models/
│           ├── User.kt
│           ├── Family.kt
│           ├── FamilyMembership.kt
│           ├── PasswordReset.kt
│           └── dto/
│               ├── RegisterRequest.kt    # Validated registration DTO
│               ├── LoginRequest.kt       # Validated login DTO
│               └── AuthResponse.kt      # Unified auth response
└── exception/
    ├── GlobalExceptionHandler.kt         # Centralized error handling
    ├── EmailAlreadyExistsException.kt    # 409 Conflict
    └── InvalidCredentialsException.kt    # 401 Unauthorized
```

### Step 2: Configure Database Connection

#### 2.1 Create application.yml
Configure database connection in `src/main/resources/application.yml`:
- MySQL connection settings
- HikariCP connection pool configuration
- JPA/Hibernate settings
- Flyway migration settings

#### 2.2 Create .env File (for sensitive data)
```env
DB_URL=jdbc:mysql://localhost:3306/nakpom_db
DB_USERNAME=root
DB_PASSWORD=your_password
```

### Step 3: Create Database Schema Migrations

#### 3.1 Create Flyway Migration — Core Tables
Create `src/main/resources/db/migration/V1__Create_initial_tables.sql`:
- Create `users` table
- Create `families` table
- Create `family_memberships` table
- Add foreign key constraints
- Add indexes for performance

#### 3.2 Create Flyway Migration — Password Resets
Create `src/main/resources/db/migration/V2__Create_password_resets.sql`:
- Create `password_resets` table (email, token, expires_at)
- Add indexes on `token` and `email` for fast lookup

#### 3.3 Test Migrations
Run application to trigger Flyway migrations automatically.

### Step 4: Implement Data Models (JPA Entities)

#### 4.1 Create User Entity
- `@Entity` annotation
- Fields: userId, email, passwordHash, fullName, createdAt
- `@Table` annotation for custom table name
- JPA annotations: `@Id`, `@GeneratedValue`, `@Column`, etc.

#### 4.2 Create Family Entity
- Fields: familyId, familyName, inviteCode, createdAt
- Unique constraint on inviteCode

#### 4.3 Create FamilyMembership Entity
- Fields: membershipId, userId, familyId, role
- Many-to-one relationships to User and Family
- Composite unique constraint on (userId, familyId)

#### 4.4 Create PasswordReset Entity
- Fields: id, email, token, expiresAt
- Unique constraint on token

### Step 5: Implement Repository Layer

#### 5.1 Create UserRepository
- Extend `JpaRepository<User, Int>`
- Custom query methods: `findByEmail`, `existsByEmail`

#### 5.2 Create FamilyRepository
- Extend `JpaRepository<Family, Int>`
- Custom queries: `findByInviteCode`, `existsByInviteCode`

#### 5.3 Create FamilyMembershipRepository
- Extend `JpaRepository<FamilyMembership, Int>`
- Custom queries: `findByUserIdAndFamilyId`, `findByUserId`, `findByFamilyId`, `existsByUserIdAndFamilyId`

#### 5.4 Create PasswordResetRepository
- Extend `JpaRepository<PasswordReset, Int>`
- Custom queries: `findByToken`, `findByEmail`, `deleteByEmail`

### Step 6: Configure Application Properties

#### 6.1 Update application.yml
- Server port configuration (8080)
- JPA show-sql (true for development)
- Hibernate ddl-auto (validate for production, update for dev)
- Logging configuration

### Step 7: Set Up Git Repository

#### 7.1 Initialize Git
```bash
git init
```

#### 7.2 Create .gitignore
Ignore:
- `.env` file (contains sensitive data)
- `build/` directory
- `.gradle/` directory
- IDE files (.idea/, .vscode/)
- OS files (.DS_Store, Thumbs.db)
- Log files

#### 7.3 Create Initial Commit
```bash
git add .
git commit -m "Initial Sprint 1 scaffolding: Spring Boot backend with layered architecture"
```

#### 7.4 Create GitHub Repository (Instructions)
- Go to GitHub/GitLab
- Create new repository named "nakpom-backend"
- Copy remote repository URL
- Add remote: `git remote add origin <URL>`
- Push: `git push -u origin main`

### Step 8: Generate Gradle Wrapper

#### 8.1 Install Gradle (if not installed)
```bash
sudo snap install gradle  # or
sudo apt install gradle
```

#### 8.2 Generate Gradle Wrapper
```bash
gradle wrapper --gradle-version 8.5
```

---

## Part B: Core Business Logic (PDF Sprint 1 Requirements)

### Step 9: Add Request/Response DTOs with Validation

#### 9.1 Create RegisterRequest DTO
- `@NotBlank` on email, password, fullName
- `@Email` format validation on email
- `@Size(min=6, max=128)` on password
- `@Size(min=2, max=100)` on fullName

#### 9.2 Create LoginRequest DTO
- `@NotBlank` on email, password
- `@Email` format validation on email

#### 9.3 Create AuthResponse DTO
- Fields: userId, email, fullName, familyId, familyName, inviteCode, message, timestamp
- Used for both register and login responses

### Step 10: Add Custom Exception Handling

#### 10.1 Create EmailAlreadyExistsException
- Extends `RuntimeException`
- Thrown when a duplicate email is detected during registration

#### 10.2 Create InvalidCredentialsException
- Extends `RuntimeException`
- Thrown on failed login — intentionally vague message to prevent user enumeration

#### 10.3 Update GlobalExceptionHandler
- `MethodArgumentNotValidException` → 400 Bad Request (validation errors with field details)
- `EmailAlreadyExistsException` → 409 Conflict
- `InvalidCredentialsException` → 401 Unauthorized
- `Exception` (catch-all) → 500 Internal Server Error

### Step 11: Implement AuthService — Registration with "Krousa Me" Hook

#### 11.1 Implement registerUser()
Transactional flow:
1. Check email uniqueness → throw `EmailAlreadyExistsException` if taken
2. Hash password with BCrypt (cost factor 12)
3. Save `User` entity
4. Generate unique invite code (format: `NP-XXXX`, alphanumeric, avoids ambiguous chars 0/O/1/I)
5. Create `Family("Krousa Me")` with the generated invite code
6. Create `FamilyMembership(userId, familyId, role="owner")`
7. Return `AuthResponse` with user + family details

#### 11.2 Implement loginUser()
Flow:
1. Find user by email → throw `InvalidCredentialsException` if not found
2. Verify password with `BCrypt.checkpw()` → throw `InvalidCredentialsException` if mismatch
3. Fetch user's primary family (first owned family)
4. Return `AuthResponse` with user + family details

### Step 12: Implement Controller Layer

#### 12.1 Create AuthController
- `POST /api/v1/auth/register` → accepts `@Valid RegisterRequest`, returns 201 Created
- `POST /api/v1/auth/login` → accepts `@Valid LoginRequest`, returns 200 OK

#### 12.2 Create Health Check Controller
- `GET /api/v1/health` endpoint to verify server is running
- Returns JSON with status, timestamp, service name, and version

---

## Part C: Documentation & Verification

### Step 13: Create UML Diagrams
Located in `docs/uml-diagrams.md`:
- **Use Case Diagram**: Register, Login, Auto-Create "Krousa Me", Generate Invite Code, Health Check
- **Class Diagram**: All entities, DTOs, repositories, services, controllers, and exception handlers
- **Sequence Diagram**: Full registration flow (Client → Controller → Service → Repositories → DB)
- **ER Diagram**: users, families, family_memberships, password_resets with relationships

### Step 14: Create README.md
- Quick start guide (prerequisites, clone, configure, build, run)
- API endpoint documentation with curl examples
- Project structure overview
- Team guide (Backend Dev, DB Dev, Frontend Dev)

### Step 15: Verify Setup

#### 15.1 Build Project
```bash
./gradlew clean build
```

#### 15.2 Run Application
```bash
./gradlew bootRun
```

#### 15.3 Test Health Endpoint
```bash
curl http://localhost:8080/api/v1/health
```

#### 15.4 Test Registration
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"secure123","fullName":"Test User"}'
```

#### 15.5 Test Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"secure123"}'
```

#### 15.6 Verify Database
- Connect to MySQL
- Check that all 4 tables were created (users, families, family_memberships, password_resets)
- Verify foreign key constraints
- Confirm auto-created "Krousa Me" family after registration

### Step 16: Team Alignment

#### 16.1 Backend Developer
- Can test health endpoint and auth endpoints
- Knows where to add new controllers/services

#### 16.2 Database Developer
- Knows repository layer location
- Can write new migrations in db/migration/

#### 16.3 Frontend Developer
- Has server endpoint: `http://localhost:8080`
- Can configure networking library to point to local IP
- API base URL: `http://<local-ip>:8080/api/v1`

---

## Success Criteria
- [x] Spring Boot project structure created with layered architecture
- [x] Database configuration in application.yml
- [x] .env file created for sensitive data
- [x] Flyway migration scripts created (V1: 3 core tables, V2: password_resets)
- [x] JPA entity models implemented (User, Family, FamilyMembership, PasswordReset)
- [x] Repository layer implemented (User, Family, FamilyMembership, PasswordReset)
- [x] Request/Response DTOs with Bean Validation (RegisterRequest, LoginRequest, AuthResponse)
- [x] Custom exceptions and centralized error handling (400, 401, 409, 500)
- [x] AuthService: registerUser() with BCrypt hashing and "Krousa Me" auto-creation
- [x] AuthService: loginUser() with BCrypt verification
- [x] AuthController: POST /register (201) and POST /login (200)
- [x] HealthController: GET /health returns 200 OK
- [x] .gitignore properly excludes sensitive files
- [x] Git repository initialized and committed
- [x] Gradle wrapper generated
- [x] Spring Boot application builds successfully
- [x] UML diagrams created (Use Case, Class, Sequence, ER)
- [x] README.md with team onboarding and API docs
- [x] MySQL database has all 4 tables created
- [x] Foreign key constraints are working
- [x] Team members can run the application locally
