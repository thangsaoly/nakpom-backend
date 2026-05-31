# Sprint 2: The Invite Engine, Security Walls & UI Scaffold (Week 2)

This plan seamlessly integrates the password reset deliverables left over from Sprint 1 with the core Sprint 2 objectives. It is fully tailored to the backend stack (Spring Boot 3.2.5 + Spring Data JPA + Resend SDK) and the native Android frontend stack (Jetpack Compose).

## Technology Stack
- **Backend**: Spring Boot 3.2.5 with Kotlin
- **ORM**: Spring Data JPA with Hibernate
- **Email**: Resend Java SDK `com.resend:resend-java:3.1.0`
- **Security**: Spring `HandlerInterceptor` (custom membership gate)
- **Frontend**: Android (Jetpack Compose)
- **Networking (Android)**: Ktor or Retrofit
- **State Management**: Kotlin `MutableStateFlow` + MVVM/MVI

---

## Sprint 2 Overview & Goals

- **Sprint Timeline**: Week 2
- **Core Objective**: Complete the secure auth lifecycle (Password Reset), deploy the **Family Invite Engine** backend with structural data isolation walls, and scaffold the modular architecture of the Jetpack Compose app along with all primary authentication screens.

---

## Part A: Backend Engineering Tasks

### Phase 1: Password Reset Completion (Leftover Catch-up)

The database tables and HTML email template dispatchers are already verified from Sprint 1. This phase builds out the remaining transaction logic layers.

#### Step 1: Implement Password Reset Logic in AuthService.kt

##### 1.1 Implement `requestPasswordReset(email)`
Transactional flow:
1. Verify if the user email exists in the database.
   - If not found: throw a safe, generic success **or** a specific 404 handler depending on security preference (to prevent user enumeration).
2. Generate a cryptographically secure, random alphanumeric token string.
3. Save/overwrite the record in the `password_resets` table with `expiresAt = LocalDateTime.now().plusMinutes(15)`.
4. Pass the token and email destination to `EmailService.sendResetEmail()`.

##### 1.2 Implement `resetPassword(token, newPassword)`
Transactional flow:
1. Fetch the token record from `PasswordResetRepository`.
2. Verify the record exists **and** that `LocalDateTime.now().isBefore(expiresAt)`.
3. Cryptographically hash the incoming `newPassword` via BCrypt (cost factor 12).
4. Locate the associated user profile, update their `password_hash` column.
5. Delete the token from the `password_resets` table to prevent re-use attacks.

#### Step 2: Wire HTTP Controller Mappings (AuthController.kt)

- **`POST /api/v1/auth/forgot-password`** — accepts `@Valid ForgotPasswordDto(email)`.
- **`POST /api/v1/auth/reset-password`** — accepts `@Valid ResetPasswordDto(token, newPassword)`.

Add the corresponding DTOs with Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`).

#### Full Reset Flow (Reference)

```
[User taps "Forgot Password"]
  → POST /forgot-password { email }
  → Backend generates token, saves to password_resets with 15-min expiry, sends email

[User opens email, clicks Reset Password button]
  → Link: https://nakpom.com/reset-password?token=XXXXXX
  → App opens "Enter New Password" screen with token pre-filled

[User submits new password]
  → POST /reset-password { token, newPassword }
  → Backend validates token + expiry
  → Updates password_hash in users table
  → Deletes token from password_resets
  → Returns 200 OK → app shows "Password changed successfully"
```

---

### Phase 2: The Invite Engine & Join Family Logic

Deploy the core mechanism allowing families to merge using the **6-character secure invite code** established in Sprint 1.

#### Step 3: Implement Join Logic in FamilyService.kt

##### 3.1 Implement `joinFamilyByCode(userId: Long, code: String)`
Transactional flow:
1. Query the `families` table by `invite_code`.
   - If not found: throw a `ResourceNotFoundException` (resolves to a clean 404 via the centralized exception handler).
2. Check if a record already exists in `family_memberships` matching that specific `userId` and `familyId` — to prevent duplicate entry constraint violations.
3. If clear: insert a new row into `family_memberships` with `role = "member"`.
4. Return basic family metadata (familyId, familyName, inviteCode).

#### Step 4: Wire HTTP Controller Mapping (FamilyController.kt)

- **`GET /api/v1/family/join?code=...`** — query parameter accepts the 6-character invite code.
- Extract the active `userId` from the authentication principal **or** from a test identification request header (`X-User-Id`) during development.

**Exception to add:**

| Exception Class | HTTP Status | Trigger |
|---|---|---|
| `ResourceNotFoundException` | 404 Not Found | Invite code does not match any family |
| `MembershipAlreadyExistsException` | 409 Conflict | User is already a member of that family |

---

### Phase 3: The Membership Security Filter (Feed Interceptor)

To guarantee complete data privacy, a lightweight execution interceptor must prevent users from calling protected paths like `/api/v1/family/{familyId}/feed` unless they are explicitly authorized members.

#### Step 5: Build a Custom Spring HandlerInterceptor

##### 5.1 Create `FamilyMembershipInterceptor`
- Create a `@Component` class: `FamilyMembershipInterceptor : HandlerInterceptor`
- Inside `preHandle`:
  1. Check if the request URI contains the `{familyId}` path variable.
  2. Extract the `userId` from the request (principal or test header).
  3. Call `FamilyMembershipRepository.existsByUserIdAndFamilyId(userId, familyId)`.
  4. If the result is `false`: immediately throw an `AccessDeniedException` → returns a clean `403 Forbidden` before any data query executes.

#### Step 6: Register the Interceptor

In your global `WebMvcConfigurer` configuration class:
- Append `FamilyMembershipInterceptor` via `addInterceptors(registry)`.
- Scope it to paths targeting family feeds and message boards: `/api/v1/family/**`.

```kotlin
@Configuration
class WebConfig(
    private val familyMembershipInterceptor: FamilyMembershipInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(familyMembershipInterceptor)
            .addPathPatterns("/api/v1/family/**")
    }
}
```

---

## Part B: Android Frontend Engineering Tasks

> **Architecture Decision**: The Android frontend will be developed in a **separate repository** and connect to this backend exclusively through the REST API endpoints defined in Part A.
>
> This backend repository (`nakpom-backend`) is API-only. No frontend code lives here.
>
> See the Android project repository for Jetpack Compose UI implementation.

---

## Part C: Documentation & Verification

### Step 13: Update UML Diagrams

Extend `docs/uml-diagrams.md` with Sprint 2 additions:

- **Sequence Diagram**: Password reset full flow (request → email → token validation → password update).
- **Sequence Diagram**: Family join via invite code.
- **Component Diagram**: `FamilyMembershipInterceptor` positioned in the Spring MVC request pipeline.

### Step 14: Update Database Documentation

Extend `docs/database-schema.md`:

- Confirm no new Flyway migrations are required (all tables exist from Sprint 1: `users`, `families`, `family_memberships`, `password_resets`).
- Document the `FamilyMembershipRepository.existsByUserIdAndFamilyId()` query as a security-critical lookup.

### Step 15: Verify Backend Endpoints

#### 15.1 Test Forgot Password
```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```
Expected: `200 OK` + reset email delivered to inbox.

#### 15.2 Test Reset Password
```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token":"XXXXXX","newPassword":"newSecure456"}'
```
Expected: `200 OK`, old password invalidated, token deleted from `password_resets`.

#### 15.3 Test Join Family by Invite Code
```bash
curl -X GET "http://localhost:8080/api/v1/family/join?code=NP-AB12CD" \
  -H "X-User-Id: 2"
```
Expected: `200 OK` with family metadata. Repeated call → `409 Conflict`.

#### 15.4 Test Membership Interceptor
```bash
curl -X GET http://localhost:8080/api/v1/family/1/feed \
  -H "X-User-Id: 99"
```
Expected: `403 Forbidden` for a user not in that family.

#### 15.5 Verify Database State
- `password_resets` token is deleted after a successful reset.
- `family_memberships` has a new row with `role = "member"` after a successful join.

---

## Runtime Troubleshooting Notes

### Token expiry check fails silently
**Symptom**: Expired tokens are still accepted.

**Cause**: Comparing `LocalDateTime` without accounting for timezone context.

**Fix**: Use `LocalDateTime.now()` consistently on both save and comparison sides. Do not mix `Instant` and `LocalDateTime`.

### Interceptor not triggering
**Symptom**: Unauthenticated users can reach protected family routes.

**Cause**: Interceptor not registered or path pattern mismatch.

**Fix**: Confirm `addPathPatterns("/api/v1/family/**")` is set in `WebMvcConfigurer`. Verify the interceptor bean is annotated with `@Component` and injected correctly.

### Duplicate membership insert
**Symptom**: `DataIntegrityViolationException` when a user tries to join a family they already belong to.

**Cause**: Missing pre-check before inserting into `family_memberships`.

**Fix**: Call `FamilyMembershipRepository.existsByUserIdAndFamilyId(userId, familyId)` before `save()`. Throw `MembershipAlreadyExistsException` (→ 409 Conflict) if true.

---

## Success Criteria

### Backend Track (this repository)
- [ ] `AuthService.requestPasswordReset(email)` — generates secure token, saves with 15-min expiry, dispatches email
- [ ] `AuthService.resetPassword(token, newPassword)` — validates token, BCrypt-hashes new password, clears token
- [ ] `POST /api/v1/auth/forgot-password` endpoint wired and validated
- [ ] `POST /api/v1/auth/reset-password` endpoint wired and validated
- [ ] `FamilyService.joinFamilyByCode(userId, code)` — full join flow with duplicate guard
- [ ] `GET /api/v1/family/join?code=...` endpoint wired and validated
- [ ] `FamilyMembershipInterceptor` built and blocks non-members with 403
- [ ] Interceptor registered on `/api/v1/family/**` in `WebMvcConfigurer`
- [ ] `ResourceNotFoundException` and `MembershipAlreadyExistsException` added to `GlobalExceptionHandler`

### Frontend Track
> Implemented in the separate Android repository. Connects to the endpoints above.
