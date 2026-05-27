# NakPom (អ្នកភូមិ) — Backend

A hyper-local social ecosystem designed to strengthen family and community bonds in Cambodia.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Kotlin 1.9 + JVM 17 |
| Framework | Spring Boot 3.2.5 |
| Build | Gradle (Kotlin DSL) |
| Database | MySQL 8.x |
| Migrations | Flyway |
| Connection Pool | HikariCP |
| ORM | Spring Data JPA / Hibernate |
| Password Hashing | BCrypt (jbcrypt) |

## Quick Start

### Prerequisites
- JDK 17+
- MySQL 8.x running locally
- A database named `nakpom_db`

### 1. Clone & Configure
```bash
git clone <repository-url>
cd nakpom-backend
```

Create a `.env` file in the project root:
```env
DB_URL=jdbc:mysql://localhost:3306/nakpom_db
DB_USERNAME=root
DB_PASSWORD=your_password
```

### 2. Build
```bash
./gradlew clean build
```

### 3. Run
```bash
./gradlew bootRun
```

The server starts on `http://localhost:8080`.

### 4. Verify
```bash
curl http://localhost:8080/api/v1/health
```

Expected response:
```json
{
  "status": "UP",
  "service": "nakpom-backend",
  "version": "0.0.1-SNAPSHOT"
}
```

## API Endpoints (Sprint 1)

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| `GET` | `/api/v1/health` | Server health check | ✅ Implemented |
| `POST` | `/api/v1/auth/register` | Register user + auto-create "Krousa Me" family | ✅ Implemented |
| `POST` | `/api/v1/auth/login` | Authenticate user | ✅ Implemented |

### Register
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "sokha@example.com",
    "password": "securepass123",
    "fullName": "សុខា មាន"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "sokha@example.com",
    "password": "securepass123"
  }'
```

## Project Structure

```
src/main/kotlin/com/nakpom/
├── NakPomApplication.kt             # Entry point
├── config/
│   ├── DatabaseConfig.kt            # DB configuration
│   └── SecurityConfig.kt            # Security placeholder
├── features/
│   └── auth/
│       ├── routing/
│       │   ├── AuthController.kt    # Auth endpoints
│       │   └── HealthController.kt  # Health check
│       ├── service/
│       │   └── AuthService.kt       # Business logic
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
│               ├── RegisterRequest.kt
│               ├── LoginRequest.kt
│               └── AuthResponse.kt
└── exception/
    ├── GlobalExceptionHandler.kt
    ├── EmailAlreadyExistsException.kt
    └── InvalidCredentialsException.kt
```

## Database Schema

Four tables managed via Flyway migrations:
- `users` — core user accounts
- `families` — family circle spaces
- `family_memberships` — many-to-many user↔family link (security wall)
- `password_resets` — SMTP token infrastructure for password recovery

See [UML Diagrams](docs/uml-diagrams.md) for the full ER and class diagrams.

## Team Guide

| Role | What You Need |
|------|---------------|
| **Backend Dev** | Run `./gradlew bootRun`, test endpoints at `localhost:8080` |
| **DB Dev** | Add migrations in `src/main/resources/db/migration/`, use repos in `repository/` |
| **Frontend Dev** | Base URL: `http://<local-ip>:8080/api/v1` — configure networking library to point here |
