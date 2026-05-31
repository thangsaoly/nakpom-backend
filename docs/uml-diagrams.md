# NakPom — UML Diagrams (Sprint 1 & Sprint 2)

This document contains the four required UML diagrams for Sprint 1: **Use Case**, **Activity**, **Class**, and **Sequence** diagrams. Each diagram is accompanied by a detailed description explaining the design decisions, actors, and interactions modelled.

---

## 1. Use Case Diagram

### Diagram

```mermaid
graph TB
    subgraph SystemBoundary["NakPom V1 — Sprint 1 System Boundary"]
        UC1(["UC-1: Register Account"])
        UC2(["UC-2: Login"])
        UC3(["UC-3: Auto-Create Krousa Me Space"])
        UC4(["UC-4: Generate Invite Code"])
        UC5(["UC-5: Validate Credentials"])
        UC6(["UC-6: Hash Password"])
        UC7(["UC-7: Health Check"])
    end

    User((User))
    System((System))

    User --> UC1
    User --> UC2
    UC1 -.->|"«include»"| UC5
    UC1 -.->|"«include»"| UC6
    UC1 -.->|"«include»"| UC3
    UC3 -.->|"«include»"| UC4
    UC2 -.->|"«include»"| UC5
    System --> UC7

    style UC1 fill:#4CAF50,color:#fff,stroke:#388E3C
    style UC2 fill:#2196F3,color:#fff,stroke:#1565C0
    style UC3 fill:#FF9800,color:#fff,stroke:#E65100
    style UC4 fill:#FF9800,color:#fff,stroke:#E65100
    style UC5 fill:#9C27B0,color:#fff,stroke:#6A1B9A
    style UC6 fill:#9C27B0,color:#fff,stroke:#6A1B9A
    style UC7 fill:#607D8B,color:#fff,stroke:#37474F
```

### Description

**Purpose**: Maps the functional requirements of Sprint 1 to actors and their goals within the system boundary.

**Actors**:

| Actor | Type | Description |
|-------|------|-------------|
| **User** | Primary | A Cambodian citizen interacting with the NakPom mobile app. Initiates registration and login. |
| **System** | Supporting | The NakPom backend server. Performs automated health monitoring and internal processes. |

**Use Cases**:

| ID | Use Case | Actor | Pre-condition | Post-condition | Description |
|----|----------|-------|---------------|----------------|-------------|
| UC-1 | Register Account | User | User has no existing account with the given email. | Account created, "Krousa Me" family space exists, user linked as owner. | The user submits email, password, and full name. The system validates the input fields using Bean Validation constraints (`@Email`, `@NotBlank`, `@Size`), checks that the email is not already registered, hashes the password using BCrypt with a cost factor of 12, persists the User record, and triggers the automatic creation of the default family space. Returns a 201 response with user and family details. |
| UC-2 | Login | User | User has a registered account. | User is authenticated and receives account details. | The user submits email and password. The system looks up the account by email, verifies the password against the stored BCrypt hash, and returns the user's profile along with their primary family information. Returns 200 on success, 401 on failure. |
| UC-3 | Auto-Create "Krousa Me" | System | User record has just been persisted in the database. | A Family record named "Krousa Me" exists, and a FamilyMembership record links the user to it with role "owner". | This is an internal use case included by UC-1. It runs within the same `@Transactional` boundary as registration. The system generates a unique invite code, creates the Family entity, and inserts the ownership membership link. If any step fails, the entire registration transaction rolls back. |
| UC-4 | Generate Invite Code | System | A new family is being created. | A unique `NP-XXXXXX` code is assigned to the family. | Generates a secure random 6-character uppercase alphanumeric code prefixed with `NP-`. The system checks for uniqueness against the `families` table and retries if a collision is detected (up to 10 attempts). The 6-character format provides over 2 billion possible combinations. |
| UC-5 | Validate Credentials | System | A registration or login request has been received. | Input is confirmed valid, or a 400/401 error is returned. | Shared validation logic. For registration: enforces email format, password length (6–128 chars), and name length (2–100 chars). For login: verifies the provided password matches the stored BCrypt hash. Invalid input returns structured error details per field. |
| UC-6 | Hash Password | System | A plaintext password has been validated. | A BCrypt hash string is produced. | Uses `BCrypt.hashpw()` with `gensalt(12)` to produce a salted, one-way hash. The cost factor of 12 means 2¹² = 4096 iterations, balancing security and performance. |
| UC-7 | Health Check | System | Server is running. | HTTP 200 returned with server status. | A monitoring endpoint at `GET /api/v1/health` that returns the service name, version, status, and current timestamp. Used by operations tooling to confirm the backend is alive. |

**Relationships**:
- UC-1 **«include»** UC-5, UC-6, UC-3 — Registration always validates, hashes, and creates the family space.
- UC-3 **«include»** UC-4 — Family creation always generates an invite code.
- UC-2 **«include»** UC-5 — Login always validates credentials.

---

## 2. Activity Diagram

### 2.1 Registration Activity

```mermaid
flowchart TD
    Start([Start]) --> ReceiveRequest["Receive POST /api/v1/auth/register"]
    ReceiveRequest --> ValidateInput{"Validate input\n(@Valid)"}

    ValidateInput -->|"Invalid"| ReturnValidationError["Return 400\nValidation errors per field"]
    ReturnValidationError --> End1([End])

    ValidateInput -->|"Valid"| CheckEmail{"Email already\nregistered?"}
    CheckEmail -->|"Yes"| ReturnConflict["Return 409\nEmailAlreadyExistsException"]
    ReturnConflict --> End2([End])

    CheckEmail -->|"No"| HashPassword["Hash password\nBCrypt.hashpw(password, gensalt(12))"]
    HashPassword --> SaveUser["Save User to DB\nuserRepository.save()"]
    SaveUser --> GenerateCode["Generate invite code\nNP-XXXXXX"]
    GenerateCode --> CheckCodeUnique{"Code already\nexists?"}
    CheckCodeUnique -->|"Yes"| GenerateCode
    CheckCodeUnique -->|"No"| CreateFamily["Create Family\nname = 'Krousa Me'"]
    CreateFamily --> CreateMembership["Create FamilyMembership\nrole = 'owner'"]
    CreateMembership --> BuildResponse["Build AuthResponse\n(user + family details)"]
    BuildResponse --> Return201["Return 201 Created"]
    Return201 --> End3([End])

    style Start fill:#4CAF50,color:#fff
    style End1 fill:#f44336,color:#fff
    style End2 fill:#f44336,color:#fff
    style End3 fill:#4CAF50,color:#fff
    style ValidateInput fill:#FFC107,color:#000
    style CheckEmail fill:#FFC107,color:#000
    style CheckCodeUnique fill:#FFC107,color:#000
    style HashPassword fill:#7E57C2,color:#fff
    style SaveUser fill:#2196F3,color:#fff
    style CreateFamily fill:#2196F3,color:#fff
    style CreateMembership fill:#2196F3,color:#fff
```

### 2.2 Login Activity

```mermaid
flowchart TD
    Start([Start]) --> ReceiveRequest["Receive POST /api/v1/auth/login"]
    ReceiveRequest --> ValidateInput{"Validate input\n(@Valid)"}

    ValidateInput -->|"Invalid"| ReturnValidationError["Return 400\nValidation errors"]
    ReturnValidationError --> End1([End])

    ValidateInput -->|"Valid"| FindUser{"Find user\nby email?"}
    FindUser -->|"Not found"| Return401a["Return 401\nInvalidCredentialsException"]
    Return401a --> End2([End])

    FindUser -->|"Found"| VerifyPassword{"BCrypt.checkpw()\npassword matches hash?"}
    VerifyPassword -->|"No"| Return401b["Return 401\nInvalidCredentialsException"]
    Return401b --> End3([End])

    VerifyPassword -->|"Yes"| FetchFamily["Fetch user's primary family\n(first owned family)"]
    FetchFamily --> BuildResponse["Build AuthResponse\n(user + family details)"]
    BuildResponse --> Return200["Return 200 OK"]
    Return200 --> End4([End])

    style Start fill:#2196F3,color:#fff
    style End1 fill:#f44336,color:#fff
    style End2 fill:#f44336,color:#fff
    style End3 fill:#f44336,color:#fff
    style End4 fill:#4CAF50,color:#fff
    style ValidateInput fill:#FFC107,color:#000
    style FindUser fill:#FFC107,color:#000
    style VerifyPassword fill:#FFC107,color:#000
```

### Description

**Purpose**: Models the step-by-step flow of the two core Sprint 1 operations — Registration and Login — showing decision points, error paths, and the transactional boundary of the "Krousa Me" automator.

**Registration Flow (2.1)**:

The registration activity begins when the client sends a `POST /api/v1/auth/register` request with a JSON body containing `email`, `password`, and `fullName`. The flow passes through three decision gates:

1. **Input Validation** — Spring's `@Valid` annotation triggers Bean Validation on the `RegisterRequest` DTO. If any field fails (e.g., malformed email, password shorter than 6 characters), the `GlobalExceptionHandler` catches the `MethodArgumentNotValidException` and returns a 400 response with per-field error details. The request never reaches the service layer.

2. **Email Uniqueness** — The service queries `userRepository.existsByEmail()`. If the email is already taken, an `EmailAlreadyExistsException` is thrown, resulting in a 409 Conflict response.

3. **Invite Code Uniqueness** — After generating a random `NP-XXXXXX` code, the service checks `familyRepository.existsByInviteCode()`. On collision, it loops and regenerates (up to 10 attempts before falling back to an 8-character code).

The purple-highlighted step (password hashing) is a CPU-intensive operation using BCrypt with cost factor 12. The blue-highlighted steps (Save User, Create Family, Create Membership) all execute within a single `@Transactional` boundary — if any database write fails, all three are rolled back atomically.

**Login Flow (2.2)**:

The login activity is simpler with two decision gates (user existence and password verification). Importantly, both the "user not found" and "wrong password" paths return the same generic 401 error message (`"Invalid email or password"`). This is a deliberate security decision to prevent **user enumeration attacks** — an attacker cannot distinguish between a non-existent account and a wrong password.

---

## 3. Class Diagram

### Diagram

```mermaid
classDiagram
    direction TB

    class User {
        <<Entity>>
        -Int? userId
        -String email
        -String passwordHash
        -String fullName
        -LocalDateTime createdAt
    }

    class Family {
        <<Entity>>
        -Int? familyId
        -String familyName
        -String inviteCode
        -LocalDateTime createdAt
    }

    class FamilyMembership {
        <<Entity>>
        -Int? membershipId
        -Int userId
        -Int familyId
        -String role
        -LocalDateTime createdAt
    }

    class PasswordReset {
        <<Entity>>
        -Int? id
        -String email
        -String token
        -LocalDateTime expiresAt
    }

    class RegisterRequest {
        <<DTO>>
        -String email
        -String password
        -String fullName
    }

    class LoginRequest {
        <<DTO>>
        -String email
        -String password
    }

    class AuthResponse {
        <<DTO>>
        -Int userId
        -String email
        -String fullName
        -Int? familyId
        -String? familyName
        -String? inviteCode
        -String message
        -LocalDateTime timestamp
    }

    class UserRepository {
        <<interface>>
        +findByEmail(email) Optional~User~
        +existsByEmail(email) Boolean
    }

    class FamilyRepository {
        <<interface>>
        +findByInviteCode(code) Optional~Family~
        +existsByInviteCode(code) Boolean
    }

    class FamilyMembershipRepository {
        <<interface>>
        +findByUserIdAndFamilyId(uid, fid) Optional~FamilyMembership~
        +findByUserId(uid) List~FamilyMembership~
        +findByFamilyId(fid) List~FamilyMembership~
        +existsByUserIdAndFamilyId(uid, fid) Boolean
    }

    class PasswordResetRepository {
        <<interface>>
        +findByToken(token) Optional~PasswordReset~
        +findByEmail(email) List~PasswordReset~
        +deleteByEmail(email) void
    }

    class AuthService {
        <<Service>>
        -UserRepository userRepository
        -FamilyRepository familyRepository
        -FamilyMembershipRepository membershipRepo
        +registerUser(RegisterRequest) AuthResponse
        +loginUser(LoginRequest) AuthResponse
        -generateUniqueInviteCode() String
    }

    class AuthController {
        <<RestController>>
        -AuthService authService
        +registerUser(RegisterRequest) ResponseEntity~AuthResponse~
        +loginUser(LoginRequest) ResponseEntity~AuthResponse~
    }

    class HealthController {
        <<RestController>>
        +healthCheck() ResponseEntity~Map~
    }

    class GlobalExceptionHandler {
        <<ControllerAdvice>>
        +handleValidationException(ex) ResponseEntity~Map~
        +handleEmailAlreadyExists(ex) ResponseEntity~Map~
        +handleInvalidCredentials(ex) ResponseEntity~Map~
        +handleException(ex) ResponseEntity~Map~
    }

    class EmailAlreadyExistsException {
        <<Exception>>
    }

    class InvalidCredentialsException {
        <<Exception>>
    }

    UserRepository ..> User : manages
    FamilyRepository ..> Family : manages
    FamilyMembershipRepository ..> FamilyMembership : manages
    PasswordResetRepository ..> PasswordReset : manages

    AuthService --> UserRepository : depends on
    AuthService --> FamilyRepository : depends on
    AuthService --> FamilyMembershipRepository : depends on
    AuthService ..> RegisterRequest : consumes
    AuthService ..> LoginRequest : consumes
    AuthService ..> AuthResponse : produces
    AuthService ..> EmailAlreadyExistsException : throws
    AuthService ..> InvalidCredentialsException : throws

    AuthController --> AuthService : delegates to
    AuthController ..> RegisterRequest : receives
    AuthController ..> LoginRequest : receives
    AuthController ..> AuthResponse : returns

    GlobalExceptionHandler ..> EmailAlreadyExistsException : catches
    GlobalExceptionHandler ..> InvalidCredentialsException : catches

    FamilyMembership --> User : userId FK
    FamilyMembership --> Family : familyId FK
    PasswordReset --> User : email reference
```

### Description

**Purpose**: Shows the static structure of the Sprint 1 codebase — all classes, their attributes, methods, stereotypes, and relationships.

**Architectural Layers**:

The class diagram follows a layered architecture with clear separation of concerns:

| Layer | Classes | Responsibility |
|-------|---------|---------------|
| **Routing** (Controller) | `AuthController`, `HealthController` | HTTP request mapping, input validation (`@Valid`), HTTP status codes. No business logic. |
| **Service** | `AuthService` | Core business rules: registration flow with "Krousa Me" auto-creation, login with BCrypt verification, invite code generation. Annotated with `@Transactional` for atomicity. |
| **Repository** | `UserRepository`, `FamilyRepository`, `FamilyMembershipRepository`, `PasswordResetRepository` | Data access interfaces extending Spring Data `JpaRepository`. Provide derived query methods. No implementation code — Spring generates the implementations at runtime. |
| **Model (Entity)** | `User`, `Family`, `FamilyMembership`, `PasswordReset` | JPA entities mapped to MySQL tables. Kotlin `data class` with the `kotlin-jpa` plugin for no-arg constructor generation. |
| **Model (DTO)** | `RegisterRequest`, `LoginRequest`, `AuthResponse` | Data Transfer Objects for API input/output. DTOs carry Bean Validation annotations and decouple the API contract from the persistence model. |
| **Exception** | `GlobalExceptionHandler`, `EmailAlreadyExistsException`, `InvalidCredentialsException` | Centralized error handling. The handler translates exceptions into structured JSON error responses with appropriate HTTP status codes. |

**Key Design Decisions**:

- **DTOs separate from Entities**: The `RegisterRequest` contains a plaintext `password` field, while the `User` entity stores `passwordHash`. This separation prevents accidental exposure of password hashes in API responses.
- **Repository interfaces only**: Spring Data JPA generates the implementation at runtime from method signatures. Custom queries like `findByEmail` are derived from naming conventions.
- **Nullable IDs**: Entity IDs are `Int?` (nullable) because they are database-generated. After `save()`, the ID is populated by Hibernate.
- **Exception hierarchy**: Custom exceptions extend `RuntimeException` so they propagate through Spring's `@Transactional` proxy and trigger rollback by default.

---

## 4. Sequence Diagram

### 4.1 Registration Sequence

```mermaid
sequenceDiagram
    autonumber
    participant C as Client (Android App)
    participant AC as AuthController
    participant V as Bean Validation
    participant AS as AuthService
    participant UR as UserRepository
    participant FR as FamilyRepository
    participant MR as MembershipRepository
    participant DB as MySQL

    C->>AC: POST /api/v1/auth/register<br/>{email, password, fullName}
    AC->>V: @Valid RegisterRequest
    V-->>AC: Validation passed

    AC->>AS: registerUser(request)
    activate AS
    Note over AS: @Transactional begins

    AS->>UR: existsByEmail(email)
    UR->>DB: SELECT COUNT(*) FROM users WHERE email = ?
    DB-->>UR: 0
    UR-->>AS: false

    AS->>AS: BCrypt.hashpw(password, gensalt(12))
    Note right of AS: CPU-intensive: 2¹² iterations

    AS->>UR: save(User(email, hash, name))
    UR->>DB: INSERT INTO users (email, password_hash, full_name) VALUES (?, ?, ?)
    DB-->>UR: Generated user_id = 1
    UR-->>AS: User(userId=1)

    AS->>AS: generateUniqueInviteCode()
    AS->>FR: existsByInviteCode("NP-A8B9C2")
    FR->>DB: SELECT COUNT(*) FROM families WHERE invite_code = ?
    DB-->>FR: 0
    FR-->>AS: false (code is unique)

    AS->>FR: save(Family("Krousa Me", "NP-A8B9C2"))
    FR->>DB: INSERT INTO families (family_name, invite_code) VALUES (?, ?)
    DB-->>FR: Generated family_id = 1
    FR-->>AS: Family(familyId=1)

    AS->>MR: save(FamilyMembership(userId=1, familyId=1, role="owner"))
    MR->>DB: INSERT INTO family_memberships (user_id, family_id, role) VALUES (?, ?, ?)
    DB-->>MR: Generated membership_id = 1

    Note over AS: @Transactional commits (3 INSERTs)
    AS-->>AC: AuthResponse(userId=1, familyId=1, inviteCode="NP-A8B9C2")
    deactivate AS
    AC-->>C: HTTP 201 Created + JSON body
```

### 4.2 Login Sequence

```mermaid
sequenceDiagram
    autonumber
    participant C as Client (Android App)
    participant AC as AuthController
    participant V as Bean Validation
    participant AS as AuthService
    participant UR as UserRepository
    participant MR as MembershipRepository
    participant FR as FamilyRepository
    participant DB as MySQL

    C->>AC: POST /api/v1/auth/login<br/>{email, password}
    AC->>V: @Valid LoginRequest
    V-->>AC: Validation passed

    AC->>AS: loginUser(request)
    activate AS

    AS->>UR: findByEmail(email)
    UR->>DB: SELECT * FROM users WHERE email = ?
    DB-->>UR: Row found
    UR-->>AS: User(userId=1, passwordHash="$2a$12$...")

    AS->>AS: BCrypt.checkpw(password, storedHash)
    Note right of AS: Returns true if match

    AS->>MR: findByUserId(1)
    MR->>DB: SELECT * FROM family_memberships WHERE user_id = ?
    DB-->>MR: [FamilyMembership(familyId=1, role="owner")]
    MR-->>AS: List of memberships

    AS->>FR: findById(1)
    FR->>DB: SELECT * FROM families WHERE family_id = ?
    DB-->>FR: Family("Krousa Me", "NP-A8B9C2")
    FR-->>AS: Family object

    AS-->>AC: AuthResponse(userId=1, familyName="Krousa Me")
    deactivate AS
    AC-->>C: HTTP 200 OK + JSON body
```

### 4.3 Registration Failure — Duplicate Email

```mermaid
sequenceDiagram
    autonumber
    participant C as Client (Android App)
    participant AC as AuthController
    participant AS as AuthService
    participant UR as UserRepository
    participant GEH as GlobalExceptionHandler
    participant DB as MySQL

    C->>AC: POST /api/v1/auth/register<br/>{email: "existing@mail.com", ...}
    AC->>AS: registerUser(request)
    activate AS

    AS->>UR: existsByEmail("existing@mail.com")
    UR->>DB: SELECT COUNT(*) FROM users WHERE email = ?
    DB-->>UR: 1
    UR-->>AS: true

    AS->>AS: throw EmailAlreadyExistsException
    deactivate AS

    AS--xAC: Exception propagates
    AC--xGEH: Exception caught by @RestControllerAdvice
    GEH-->>C: HTTP 409 Conflict<br/>{"error": "Email Already Exists"}
```

### Description

**Purpose**: Models the runtime interactions between objects for the three key scenarios in Sprint 1 — successful registration, successful login, and registration failure due to duplicate email.

**Registration Sequence (4.1)**:

This is the most complex interaction in Sprint 1. The sequence shows **7 participants** collaborating across 3 architectural layers:

1. **Steps 1–3 (Validation)**: The controller receives the HTTP request and delegates input validation to Spring's Bean Validation framework. The `RegisterRequest` DTO is checked against `@Email`, `@NotBlank`, and `@Size` constraints before any business logic executes. If validation fails, a `MethodArgumentNotValidException` is thrown and the `GlobalExceptionHandler` returns a 400 response — this path is not shown to keep the diagram focused on the happy path.

2. **Steps 4–9 (Uniqueness + Hashing)**: The `AuthService` checks email uniqueness with a `SELECT COUNT(*)` query. If the email is available, it hashes the plaintext password using BCrypt. The `gensalt(12)` call produces a salt with 2¹² = 4096 iterations — this is deliberately CPU-intensive to make brute-force attacks impractical.

3. **Steps 10–13 (User Persistence)**: The `User` entity is saved to MySQL. Hibernate executes an `INSERT` and MySQL returns the auto-generated `user_id`. This ID is then used in subsequent steps.

4. **Steps 14–19 (Krousa Me Automator)**: The system generates a random `NP-XXXXXX` invite code, verifies its uniqueness, creates the Family record, and links the user via a FamilyMembership with role `"owner"`. All three `INSERT` operations (steps 11, 17, 19) are wrapped in a single `@Transactional` boundary — if the membership insert fails, the user and family inserts are rolled back too.

5. **Steps 20–21 (Response)**: The `AuthResponse` DTO is built with combined user and family data, and returned as a 201 Created response.

**Login Sequence (4.2)**:

The login flow involves fewer database operations:

1. **User lookup** (step 5): A `SELECT` by email. If no row is found, `InvalidCredentialsException` is thrown immediately.
2. **Password verification** (step 8): `BCrypt.checkpw()` compares the plaintext password against the stored hash. This is a constant-time comparison to prevent timing attacks.
3. **Family resolution** (steps 9–13): The service fetches the user's memberships and resolves the primary family (the first one where `role = "owner"`). This information is included in the login response so the Android app can navigate directly to the user's family space.

**Failure Sequence (4.3)**:

Demonstrates the exception propagation chain. When `EmailAlreadyExistsException` is thrown inside `AuthService`, it propagates up through `AuthController` and is intercepted by the `GlobalExceptionHandler` (annotated with `@RestControllerAdvice`). The handler translates the exception into a structured 409 Conflict JSON response. The same pattern applies to `InvalidCredentialsException` (→ 401) and `MethodArgumentNotValidException` (→ 400).

---

## Sprint 2 Diagrams

### 5. Sequence Diagram — Password Reset Full Flow

```mermaid
sequenceDiagram
    autonumber
    participant C as Client (Android App)
    participant AC as AuthController
    participant AS as AuthService
    participant PR as PasswordResetRepository
    participant UR as UserRepository
    participant ES as EmailService
    participant GEH as GlobalExceptionHandler
    participant DB as MySQL

    Note over C,DB: Step 1 — Request Reset Email

    C->>AC: POST /api/v1/auth/forgot-password
    Note right of C: { "email": "user@example.com" }
    AC->>AS: requestPasswordReset(email)
    activate AS

    AS->>UR: existsByEmail(email)
    UR->>DB: SELECT COUNT(*) FROM users WHERE email = ?
    DB-->>UR: 1
    UR-->>AS: true

    AS->>PR: deleteByEmail(email)
    PR->>DB: DELETE FROM password_resets WHERE email = ?
    Note right of DB: Clears any previous token

    AS->>AS: SecureRandom → 32-char hex token
    AS->>PR: save(PasswordReset(email, token, now+15min))
    PR->>DB: INSERT INTO password_resets (email, token, expires_at)
    DB-->>PR: id = 1

    AS->>ES: sendResetEmail(email, token)
    ES->>ES: Build reset link with token
    ES-->>C: Email dispatched via Resend API
    deactivate AS

    AC-->>C: HTTP 200 OK
    Note right of AC: { "message": "If that email is registered..." }

    Note over C,DB: Step 2 — Submit New Password

    C->>AC: POST /api/v1/auth/reset-password
    Note right of C: { "token": "abc123...", "newPassword": "newPass" }
    AC->>AS: resetPassword(token, newPassword)
    activate AS

    AS->>PR: findByToken(token)
    PR->>DB: SELECT * FROM password_resets WHERE token = ?
    DB-->>PR: PasswordReset(email, expiresAt)
    PR-->>AS: PasswordReset record

    AS->>AS: LocalDateTime.now().isAfter(expiresAt)?
    Note right of AS: false → token is still valid

    AS->>UR: findByEmail(email)
    UR->>DB: SELECT * FROM users WHERE email = ?
    DB-->>UR: User record
    UR-->>AS: User

    AS->>AS: BCrypt.hashpw(newPassword, gensalt(12))
    AS->>UR: save(user.copy(passwordHash = newHash))
    UR->>DB: UPDATE users SET password_hash = ? WHERE user_id = ?

    AS->>PR: delete(resetRecord)
    PR->>DB: DELETE FROM password_resets WHERE id = ?
    Note right of DB: Token consumed — cannot be reused

    deactivate AS
    AC-->>C: HTTP 200 OK
    Note right of AC: { "message": "Password has been reset successfully." }
```

**Description**:

The password reset flow spans two separate HTTP requests:

1. **`POST /forgot-password`** — The user submits their email. The backend silently skips unknown emails (prevents enumeration), clears any existing token for that address, generates a new 32-character cryptographically random hex token, stores it with a 15-minute expiry, and dispatches the email via Resend. The response is always 200 OK regardless of whether the email is registered.

2. **`POST /reset-password`** — The user submits the token (from the email link) and their new password. The backend looks up the token, validates it is not expired, BCrypt-hashes the new password, updates the `users` table, and immediately deletes the token so it cannot be reused. If the token is missing or expired, `InvalidTokenException` is thrown → 400 Bad Request.

---

### 6. Sequence Diagram — Family Join via Invite Code

```mermaid
sequenceDiagram
    autonumber
    participant C as Client (Android App)
    participant INT as FamilyMembershipInterceptor
    participant FC as FamilyController
    participant FS as FamilyService
    participant FR as FamilyRepository
    participant MR as FamilyMembershipRepository
    participant GEH as GlobalExceptionHandler
    participant DB as MySQL

    C->>INT: GET /api/v1/family/join?code=NP-AB12CD
    Note right of C: Header: X-User-Id: 2

    INT->>INT: pathVariables["familyId"]?
    Note right of INT: No {familyId} in /join → return true (pass through)

    INT->>FC: Request forwarded
    FC->>FS: joinFamilyByCode(userId=2, code="NP-AB12CD")
    activate FS

    FS->>FR: findByInviteCode("NP-AB12CD")
    FR->>DB: SELECT * FROM families WHERE invite_code = ?
    DB-->>FR: Family(familyId=1, "Krousa Me")
    FR-->>FS: Family

    FS->>MR: existsByUserIdAndFamilyId(userId=2, familyId=1)
    MR->>DB: SELECT COUNT(*) FROM family_memberships WHERE user_id=2 AND family_id=1
    DB-->>MR: 0
    MR-->>FS: false (not yet a member)

    FS->>MR: save(FamilyMembership(userId=2, familyId=1, role="member"))
    MR->>DB: INSERT INTO family_memberships (user_id, family_id, role)
    DB-->>MR: membership_id = 5

    FS-->>FC: JoinFamilyResponse(familyId=1, "Krousa Me", role="member")
    deactivate FS
    FC-->>C: HTTP 200 OK + JoinFamilyResponse

    Note over C,DB: If code is wrong → 404. If already a member → 409.
```

**Description**:

The join flow passes through the `FamilyMembershipInterceptor` first. Since `/join` has no `{familyId}` path variable, the interceptor detects an empty path variable map and passes the request through immediately without any membership check.

`FamilyService.joinFamilyByCode()` then performs two sequential checks:
1. **Invite code lookup** — `findByInviteCode()` queries the `families` table. If no match, `ResourceNotFoundException` → 404.
2. **Duplicate guard** — `existsByUserIdAndFamilyId()` checks if the user already has a row in `family_memberships` for that family. If yes, `MembershipAlreadyExistsException` → 409.

Only if both checks pass is a new membership row inserted with `role = "member"`.

---

### 7. Component Diagram — FamilyMembershipInterceptor in the Request Pipeline

```mermaid
graph LR
    Client(["Client\n(Android App)"])

    subgraph SpringMVC["Spring MVC Request Pipeline"]
        Dispatcher["DispatcherServlet"]
        Interceptor["FamilyMembershipInterceptor\n(preHandle)"]
        Controller["FamilyController\nor AuthController"]
    end

    subgraph ServiceLayer["Service Layer"]
        FamilyService["FamilyService"]
        AuthService["AuthService"]
    end

    subgraph DataLayer["Data Layer"]
        MR["FamilyMembershipRepository\nexistsByUserIdAndFamilyId()"]
        DB[("MySQL\nnakpom_db")]
    end

    Client -->|"HTTP Request"| Dispatcher
    Dispatcher -->|"preHandle()"| Interceptor

    Interceptor -->|"Has {familyId}?\nNO → pass through"| Controller
    Interceptor -->|"Has {familyId}?\nYES → check membership"| MR
    MR --> DB
    DB -->|"isMember = false"| Interceptor
    Interceptor -->|"403 Forbidden"| Client
    DB -->|"isMember = true"| Interceptor
    Interceptor -->|"return true → proceed"| Controller

    Controller --> FamilyService
    Controller --> AuthService
    FamilyService --> MR

    style Interceptor fill:#FF6B35,color:#fff,stroke:#CC4400
    style MR fill:#4CAF50,color:#fff,stroke:#388E3C
    style DB fill:#2196F3,color:#fff,stroke:#1565C0
    style Client fill:#9C27B0,color:#fff,stroke:#6A1B9A
```

**Description**:

The `FamilyMembershipInterceptor` sits between `DispatcherServlet` and the target controller. It implements Spring's `HandlerInterceptor.preHandle()` which runs **before** any controller method executes.

**Decision logic**:
- If the URL does not contain a `{familyId}` path variable (e.g. `/family/join`): the interceptor returns `true` immediately — the request flows to the controller untouched.
- If `{familyId}` is present: the interceptor reads `X-User-Id` from the request header and calls `FamilyMembershipRepository.existsByUserIdAndFamilyId()`. This is a single indexed `SELECT COUNT(*)` — one DB round-trip with negligible latency.
  - Member confirmed → return `true` → controller runs normally.
  - Not a member → write a 403 JSON response directly to `HttpServletResponse` and return `false` → controller never runs.

This design means **no family data is ever queried for unauthorized users** — the block happens at the earliest possible point in the pipeline.

