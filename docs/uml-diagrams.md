# NakPom Sprint 1 — UML Diagrams

## 1. Use Case Diagram

```mermaid
graph TB
    subgraph NakPom_System["NakPom V1 System"]
        UC1["Register Account"]
        UC2["Login"]
        UC3["Auto-Create 'Krousa Me' Space"]
        UC4["Generate Invite Code"]
        UC5["Health Check"]
    end

    User((User))
    System((System))

    User --> UC1
    User --> UC2
    UC1 -->|"includes"| UC3
    UC3 -->|"includes"| UC4
    System --> UC5

    style UC1 fill:#4CAF50,color:#fff
    style UC2 fill:#2196F3,color:#fff
    style UC3 fill:#FF9800,color:#fff
    style UC4 fill:#FF9800,color:#fff
    style UC5 fill:#9E9E9E,color:#fff
```

### Use Case Descriptions

| Use Case | Actor | Description |
|----------|-------|-------------|
| Register Account | User | User submits email, password, and full name. System validates, hashes password, and creates account. |
| Auto-Create "Krousa Me" | System | Triggered on registration. Creates a private family space and links the user as owner. |
| Generate Invite Code | System | Creates a unique NP-XXXX code for the new family space. |
| Login | User | User authenticates with email and password. Returns user and family details. |
| Health Check | System | Verifies backend is running and responsive. |

---

## 2. Class Diagram

```mermaid
classDiagram
    class User {
        -Int userId
        -String email
        -String passwordHash
        -String fullName
        -LocalDateTime createdAt
    }

    class Family {
        -Int familyId
        -String familyName
        -String inviteCode
        -LocalDateTime createdAt
    }

    class FamilyMembership {
        -Int membershipId
        -Int userId
        -Int familyId
        -String role
        -LocalDateTime createdAt
    }

    class PasswordReset {
        -Int id
        -String email
        -String token
        -LocalDateTime expiresAt
    }

    class RegisterRequest {
        -String email
        -String password
        -String fullName
    }

    class LoginRequest {
        -String email
        -String password
    }

    class AuthResponse {
        -Int userId
        -String email
        -String fullName
        -Int familyId
        -String familyName
        -String inviteCode
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
        -UserRepository userRepository
        -FamilyRepository familyRepository
        -FamilyMembershipRepository membershipRepo
        +registerUser(request) AuthResponse
        +loginUser(request) AuthResponse
        -generateUniqueInviteCode() String
    }

    class AuthController {
        -AuthService authService
        +registerUser(request) ResponseEntity
        +loginUser(request) ResponseEntity
    }

    class HealthController {
        +healthCheck() ResponseEntity
    }

    class GlobalExceptionHandler {
        +handleValidationException(ex) ResponseEntity
        +handleEmailAlreadyExists(ex) ResponseEntity
        +handleInvalidCredentials(ex) ResponseEntity
        +handleException(ex) ResponseEntity
    }

    UserRepository ..> User
    FamilyRepository ..> Family
    FamilyMembershipRepository ..> FamilyMembership
    PasswordResetRepository ..> PasswordReset

    AuthService --> UserRepository
    AuthService --> FamilyRepository
    AuthService --> FamilyMembershipRepository
    AuthService ..> RegisterRequest
    AuthService ..> LoginRequest
    AuthService ..> AuthResponse

    AuthController --> AuthService
    AuthController ..> RegisterRequest
    AuthController ..> LoginRequest
    AuthController ..> AuthResponse

    FamilyMembership --> User : userId
    FamilyMembership --> Family : familyId
    PasswordReset --> User : email
```

---

## 3. Sequence Diagram — Registration Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant AS as AuthService
    participant UR as UserRepository
    participant FR as FamilyRepository
    participant MR as MembershipRepository
    participant DB as MySQL

    C->>AC: POST /api/v1/auth/register
    AC->>AC: @Valid RegisterRequest
    AC->>AS: registerUser(request)

    AS->>UR: existsByEmail(email)
    UR->>DB: SELECT COUNT(*) FROM users
    DB-->>UR: 0
    UR-->>AS: false

    AS->>AS: BCrypt.hashpw(password)

    AS->>UR: save(User)
    UR->>DB: INSERT INTO users
    DB-->>UR: user_id=1
    UR-->>AS: User(id=1)

    AS->>AS: generateUniqueInviteCode()
    AS->>FR: existsByInviteCode("NP-A3K7")
    FR->>DB: SELECT COUNT(*)
    DB-->>FR: 0
    FR-->>AS: false

    AS->>FR: save(Family "Krousa Me")
    FR->>DB: INSERT INTO families
    DB-->>FR: family_id=1
    FR-->>AS: Family(id=1)

    AS->>MR: save(FamilyMembership)
    MR->>DB: INSERT INTO family_memberships
    DB-->>MR: membership_id=1

    AS-->>AC: AuthResponse
    AC-->>C: 201 Created + JSON
```

---

## 4. ER Diagram

```mermaid
erDiagram
    USERS ||--o{ FAMILY_MEMBERSHIPS : "has"
    FAMILIES ||--o{ FAMILY_MEMBERSHIPS : "has"
    USERS ||--o{ PASSWORD_RESETS : "requests"

    USERS {
        int user_id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        timestamp created_at
    }

    FAMILIES {
        int family_id PK
        varchar family_name
        varchar invite_code UK
        timestamp created_at
    }

    FAMILY_MEMBERSHIPS {
        int membership_id PK
        int user_id FK
        int family_id FK
        varchar role
        timestamp created_at
    }

    PASSWORD_RESETS {
        int id PK
        varchar email
        varchar token UK
        timestamp expires_at
    }
```
