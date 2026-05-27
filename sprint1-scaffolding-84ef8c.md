# Sprint 1: Project Scaffolding and Environment Initialization

This plan walks through setting up a Spring Boot Kotlin backend with layered architecture, MySQL database, and Git repository for the NakPom project.

## Technology Stack
- **Backend**: Spring Boot 3.x with Kotlin
- **Build Tool**: Gradle (Kotlin DSL)
- **Database**: MySQL (local installation)
- **Connection Pool**: HikariCP (included in Spring Boot)
- **ORM**: Spring Data JPA with Hibernate
- **Migration**: Flyway (for schema versioning)

## Step 1: Initialize Spring Boot Project

### 1.1 Create Project Structure
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
- **Note**: Generate Gradle wrapper after creating build.gradle.kts

### 1.2 Create Layered Architecture Directory Structure
```
src/main/kotlin/com/nakpom/
├── NakPomApplication.kt          # Main entry point
├── config/
│   ├── DatabaseConfig.kt         # Database configuration
│   └── SecurityConfig.kt         # Security configuration (placeholder)
├── features/
│   └── auth/
│       ├── routing/
│       │   └── AuthController.kt
│       ├── service/
│       │   └── AuthService.kt
│       ├── repository/
│       │   └── UserRepository.kt
│       └── models/
│           ├── User.kt
│           ├── Family.kt
│           └── FamilyMembership.kt
└── exception/
    └── GlobalExceptionHandler.kt
```

## Step 2: Configure Database Connection

### 2.1 Create application.yml
Configure database connection in `src/main/resources/application.yml`:
- MySQL connection settings
- HikariCP connection pool configuration
- JPA/Hibernate settings
- Flyway migration settings

### 2.2 Create .env File (for sensitive data)
```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=nakpom_db
DB_USERNAME=root
DB_PASSWORD=your_password
```

## Step 3: Create Database Schema Migration

### 3.1 Create Flyway Migration Script
Create `src/main/resources/db/migration/V1__Create_initial_tables.sql`:
- Create `users` table
- Create `families` table
- Create `family_memberships` table
- Add foreign key constraints
- Add indexes for performance

### 3.2 Test Migration
Run application to trigger Flyway migration automatically.

## Step 4: Implement Data Models (JPA Entities)

### 4.1 Create User Entity
- `@Entity` annotation
- Fields: userId, email, passwordHash, fullName, createdAt
- `@Table` annotation for custom table name
- JPA annotations: `@Id`, `@GeneratedValue`, `@Column`, etc.

### 4.2 Create Family Entity
- Fields: familyId, familyName, inviteCode, createdAt
- Unique constraint on inviteCode

### 4.3 Create FamilyMembership Entity
- Fields: membershipId, userId, familyId, role
- Many-to-one relationships to User and Family
- Composite unique constraint on (userId, familyId)

## Step 5: Implement Repository Layer

### 5.1 Create UserRepository
- Extend `JpaRepository<User, Int>`
- Custom query methods: `findByEmail`, `existsByEmail`

### 5.2 Create FamilyRepository
- Extend `JpaRepository<Family, Int>`
- Custom query: `findByInviteCode`

### 5.3 Create FamilyMembershipRepository
- Extend `JpaRepository<FamilyMembership, Int>`
- Custom queries for membership lookups

## Step 6: Implement Service Layer (Placeholder)

### 6.1 Create AuthService
- Placeholder methods for future implementation
- Methods: registerUser, loginUser, createFamily, joinFamily

## Step 7: Implement Controller Layer

### 7.1 Create AuthController
- Placeholder endpoints for future implementation
- Endpoints: POST /api/v1/auth/register, POST /api/v1/auth/login

### 7.2 Create Health Check Controller
- `GET /api/v1/health` endpoint to verify server is running
- Returns simple JSON response with status

## Step 8: Configure Application Properties

### 8.1 Update application.yml
- Server port configuration (8080)
- JPA show-sql (true for development)
- Hibernate ddl-auto (validate for production, update for dev)
- Logging configuration

## Step 9: Set Up Git Repository

### 9.1 Initialize Git
```bash
git init
```

### 9.2 Create .gitignore
Ignore:
- `.env` file (contains sensitive data)
- `build/` directory
- `.gradle/` directory
- IDE files (.idea/, .vscode/)
- OS files (.DS_Store, Thumbs.db)
- Log files

### 9.3 Create Initial Commit
```bash
git add .
git commit -m "Initial Sprint 1 scaffolding: Spring Boot backend with layered architecture"
```

### 9.4 Create GitHub Repository (Instructions)
- Go to GitHub/GitLab
- Create new repository named "nakpom-backend"
- Copy remote repository URL
- Add remote: `git remote add origin <URL>`
- Push: `git push -u origin main`

## Step 10: Generate Gradle Wrapper

### 10.1 Install Gradle (if not installed)
```bash
sudo snap install gradle  # or
sudo apt install gradle
```

### 10.2 Generate Gradle Wrapper
```bash
gradle wrapper --gradle-version 8.5
```

## Step 11: Verify Setup

### 11.1 Build Project
```bash
./gradlew clean build
```

### 11.2 Run Application
```bash
./gradlew bootRun
```

### 11.3 Test Health Endpoint
```bash
curl http://localhost:8080/api/v1/health
```

### 11.4 Verify Database
- Connect to MySQL
- Check that tables were created
- Verify foreign key constraints

## Step 12: Team Alignment

### 12.1 Backend Developer
- Can test health endpoint
- Knows where to add new controllers/services

### 12.2 Database Developer
- Knows repository layer location
- Can write new migrations in db/migration/

### 12.3 Frontend Developer
- Has server endpoint: `http://localhost:8080`
- Can configure networking library to point to local IP
- API base URL: `http://<local-ip>:8080/api/v1`

## Success Criteria
- [x] Spring Boot project structure created with layered architecture
- [x] Database configuration in application.yml
- [x] .env file created for sensitive data
- [x] Flyway migration script created with all 3 tables
- [x] JPA entity models implemented (User, Family, FamilyMembership)
- [x] Repository layer implemented (UserRepository, FamilyRepository, FamilyMembershipRepository)
- [x] Service layer placeholder created (AuthService)
- [x] Controller layer created with health check endpoint
- [x] .gitignore properly excludes sensitive files
- [x] Git repository initialized and committed
- [x] Gradle wrapper generated
- [x] Spring Boot application builds successfully
- [x] Spring Boot application starts without errors
- [x] Health endpoint returns 200 OK
- [x] MySQL database has all 3 tables created (verified)
- [x] Foreign key constraints are working (verified)
- [x] Team members can run the application locally
