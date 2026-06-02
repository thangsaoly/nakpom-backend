# Sprint 3: The Private Family Feed (Week 3)

Sprint 3 transitions NakPom from an identity management utility into an active private family platform. The focus is a secure, paginated family post feed with image support, a single heart reaction (Instagram-style toggle), and comments — all gated behind the membership interceptor already in place from Sprint 2. There is no share feature.

## Technology Stack (additions)
- **File Storage**: Local filesystem (`/uploads/images/`) via Spring `MultipartFile` — swappable for cloud storage in a later sprint
- **Pagination**: Spring Data `Pageable` + `Page<T>`
- **Frontend state**: `StateFlow` + MVVM inside ViewModel

---

## Part A: Backend Engineering Tasks

### Phase 1: Database Schema Migration

#### Step 1: Write Flyway Migration V3

Create `src/main/resources/db/migration/V3__Create_posts_and_reactions.sql`:

```sql
-- Family post content (text + optional image URL)
CREATE TABLE family_posts (
    post_id    BIGINT       NOT NULL AUTO_INCREMENT,
    family_id  INT          NOT NULL,
    user_id    INT          NOT NULL,
    content_text TEXT       NULL,
    image_url  VARCHAR(512) NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id),
    FOREIGN KEY (family_id) REFERENCES families(family_id)  ON DELETE CASCADE,
    FOREIGN KEY (user_id)   REFERENCES users(user_id)       ON DELETE CASCADE
);

-- Indexes for feed query performance
CREATE INDEX idx_posts_family_created ON family_posts (family_id, created_at DESC);
CREATE INDEX idx_posts_user           ON family_posts (user_id);

-- Heart reactions — one row per (post, user) pair; only HEART is supported
CREATE TABLE post_reactions (
    post_id    BIGINT    NOT NULL,
    user_id    INT       NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, user_id),
    FOREIGN KEY (post_id)  REFERENCES family_posts(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)  REFERENCES users(user_id)        ON DELETE CASCADE
);

-- Post comments (parent_comment_id NULL = top-level; non-NULL = reply)
CREATE TABLE post_comments (
    comment_id        BIGINT    NOT NULL AUTO_INCREMENT,
    post_id           BIGINT    NOT NULL,
    user_id           INT       NOT NULL,
    parent_comment_id BIGINT    NULL,
    body              TEXT      NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id),
    FOREIGN KEY (post_id)           REFERENCES family_posts(post_id)   ON DELETE CASCADE,
    FOREIGN KEY (user_id)           REFERENCES users(user_id)           ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES post_comments(comment_id) ON DELETE CASCADE
);
CREATE INDEX idx_comments_post   ON post_comments (post_id, created_at ASC);
CREATE INDEX idx_comments_parent ON post_comments (parent_comment_id, created_at ASC);

-- Heart reactions on individual comments — same toggle pattern as post_reactions
CREATE TABLE comment_reactions (
    comment_id BIGINT    NOT NULL,
    user_id    INT       NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES post_comments(comment_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES users(user_id)             ON DELETE CASCADE
);
```

**Why `BIGINT` for `post_id`?** Posts will grow much faster than users or families. Starting with `BIGINT` avoids an INT overflow migration later.

**Why composite PK on `post_reactions(post_id, user_id)`?** One user can only react once per post (the business rule). The composite PK enforces this at the database level, making it impossible to insert a duplicate even if application code has a bug.

#### Step 2: Create JPA Entities

##### 2.1 Create `FamilyPost` Entity
File: `src/main/kotlin/com/nakpom/features/family/models/FamilyPost.kt`

Fields:
- `postId: Long?` — `@Id @GeneratedValue(strategy = IDENTITY)`
- `familyId: Int` — `@Column(name = "family_id")`
- `userId: Int` — `@Column(name = "user_id")`
- `contentText: String?` — nullable (image-only posts are valid)
- `imageUrl: String?` — nullable (text-only posts are valid)
- `createdAt: LocalDateTime` — `@Column(updatable = false)`

Validation rule: at least one of `contentText` or `imageUrl` must be non-null. Enforce in the service layer, not the entity.

##### 2.2 Create `PostReaction` Entity
File: `src/main/kotlin/com/nakpom/features/family/models/PostReaction.kt`

Use a JPA `@EmbeddedId` with a `PostReactionId` embeddable class holding `postId: Long` and `userId: Int`. This mirrors the composite PK in the database.

No `reactionType` column — the presence of a row means the user hearted the post.

##### 2.3 Create `PostComment` Entity
File: `src/main/kotlin/com/nakpom/features/family/models/PostComment.kt`

Fields:
- `commentId: Long?` — `@Id @GeneratedValue(strategy = IDENTITY)`
- `postId: Long` — `@Column(name = "post_id")`
- `userId: Int` — `@Column(name = "user_id")`
- `parentCommentId: Long?` — `@Column(name = "parent_comment_id")` — null = top-level, non-null = reply
- `body: String` — non-null, 1–1000 chars (enforce in service)
- `createdAt: LocalDateTime` — `@Column(updatable = false)`

##### 2.4 Create `CommentReaction` Entity
File: `src/main/kotlin/com/nakpom/features/family/models/CommentReaction.kt`

Use `@EmbeddedId` with `CommentReactionId(commentId: Long, userId: Int)`. Same pattern as `PostReaction`. Presence of a row = user hearted the comment.

---

### Phase 2: Repository Layer

#### Step 3: Create Repositories

##### 3.1 Create `FamilyPostRepository`
File: `src/main/kotlin/com/nakpom/features/family/repository/FamilyPostRepository.kt`

```kotlin
@Repository
interface FamilyPostRepository : JpaRepository<FamilyPost, Long> {
    // Paginated reverse-chronological feed for a family
    fun findByFamilyIdOrderByCreatedAtDesc(
        familyId: Int,
        pageable: Pageable
    ): Page<FamilyPost>

    // Count posts belonging to a family (for pagination metadata)
    fun countByFamilyId(familyId: Int): Long
}
```

##### 3.2 Create `PostReactionRepository`
File: `src/main/kotlin/com/nakpom/features/family/repository/PostReactionRepository.kt`

```kotlin
@Repository
interface PostReactionRepository : JpaRepository<PostReaction, PostReactionId> {
    // Count hearts on a post
    fun countByPostId(postId: Long): Long

    // Check whether a specific user has hearted a post
    fun existsByPostIdAndUserId(postId: Long, userId: Int): Boolean

    // Delete a heart (un-heart)
    fun deleteByPostIdAndUserId(postId: Long, userId: Int)
}
```

##### 3.3 Create `PostCommentRepository`
File: `src/main/kotlin/com/nakpom/features/family/repository/PostCommentRepository.kt`

```kotlin
@Repository
interface PostCommentRepository : JpaRepository<PostComment, Long> {
    // Top-level comments for a post
    fun findByPostIdAndParentCommentIdIsNullOrderByCreatedAtAsc(postId: Long): List<PostComment>
    fun countByPostIdAndParentCommentIdIsNull(postId: Long): Long

    // Replies to a specific comment
    fun findByParentCommentIdOrderByCreatedAtAsc(parentCommentId: Long): List<PostComment>
}
```

##### 3.4 Create `CommentReactionRepository`
File: `src/main/kotlin/com/nakpom/features/family/repository/CommentReactionRepository.kt`

```kotlin
@Repository
interface CommentReactionRepository : JpaRepository<CommentReaction, CommentReactionId> {
    fun countByCommentId(commentId: Long): Long
    fun existsByCommentIdAndUserId(commentId: Long, userId: Int): Boolean
    fun deleteByCommentIdAndUserId(commentId: Long, userId: Int)
}
```

---

### Phase 3: Service Layer

#### Step 4: Create `PostService`
File: `src/main/kotlin/com/nakpom/features/family/service/PostService.kt`

##### 4.1 Implement `createPost(familyId, userId, contentText?, imageFile?)`

Transactional flow:
1. Validate at least one of `contentText` or `imageFile` is present — throw `BadRequestException` if both are null.
2. If `imageFile` is present:
   - Validate MIME type is `image/jpeg`, `image/png`, or `image/webp`.
   - Generate a unique filename: `UUID.randomUUID().toString() + extension`.
   - Write the bytes to `/uploads/images/<filename>` on the local filesystem.
   - Store the path string in `imageUrl`.
3. Save the `FamilyPost` entity.
4. Return `FamilyPostResponse` DTO.

```kotlin
// Upload directory — inject from application.yml for easy environment switching
@Value("\${app.upload-dir:/uploads/images}")
private lateinit var uploadDir: String
```

##### 4.2 Implement `getFeed(familyId, page, size)`

Flow:
1. Build a `PageRequest.of(page, size)` pageable.
2. Call `familyPostRepository.findByFamilyIdOrderByCreatedAtDesc(familyId, pageable)`.
3. For each post, fetch heart count and comment count in one batch query.
4. Map to `FamilyPostResponse` list.
5. Return a `PagedResponse<FamilyPostResponse>` with pagination metadata (totalElements, totalPages, currentPage).

##### 4.3 Implement `toggleHeart(postId, userId)`

**Idempotent toggle** — calling twice un-hearts:

```
Incoming: userId=5, postId=12

Check existsByPostIdAndUserId(12, 5):
  - TRUE  → deleteByPostIdAndUserId(12, 5)   // un-heart
  - FALSE → save(PostReaction(postId=12, userId=5))  // heart
```

Return updated heart count + `hearted: Boolean` for the requesting user.

##### 4.4 Implement `addComment(postId, userId, body, parentCommentId?)`

1. Validate `body` is not blank and ≤ 1000 chars.
2. If `parentCommentId` is provided, verify it exists and belongs to `postId` — throw `CommentNotFoundException` otherwise.
3. Save `PostComment` entity (with `parentCommentId` set for replies).
4. Return `PostCommentResponse` DTO.

##### 4.5 Implement `getComments(postId, requestingUserId)`

- Fetch top-level comments (`parentCommentId IS NULL`) ordered by `created_at ASC`.
- For each comment, fetch its replies and heart count (+ `hearted: Boolean` for the requesting user).
- Return `List<PostCommentResponse>` where each item includes a nested `replies: List<PostCommentResponse>`.
- No pagination for MVP.

##### 4.6 Implement `toggleCommentHeart(commentId, userId)`

Same idempotent pattern as `toggleHeart`:
```
existsByCommentIdAndUserId(commentId, userId):
  - TRUE  → deleteByCommentIdAndUserId   // un-heart
  - FALSE → save(CommentReaction(...))   // heart
```
Return `CommentHeartResponse(commentId, heartCount, hearted)`.

---

### Phase 4: Controller Layer

#### Step 5: Extend `FamilyController` with Post Endpoints

Add to `src/main/kotlin/com/nakpom/features/family/routing/FamilyController.kt`:

##### 5.1 `POST /api/v1/family/{familyId}/posts`
- Accepts `@RequestParam` for `contentText` (optional) and `@RequestParam("image") MultipartFile?` (optional).
- Reads `userId` from `X-User-Id` header.
- Returns **201 Created** + `FamilyPostResponse`.

##### 5.2 `GET /api/v1/family/{familyId}/posts?page=0&size=20`
- Returns **200 OK** + `PagedResponse<FamilyPostResponse>`.
- Pagination defaults: `page=0`, `size=20`, max `size=50`.

##### 5.3 `POST /api/v1/family/{familyId}/posts/{postId}/heart`
- No request body.
- Reads `userId` from `X-User-Id` header.
- Returns **200 OK** + `HeartResponse(postId, heartCount, hearted)`.

##### 5.4 `POST /api/v1/family/{familyId}/posts/{postId}/comments`
- Accepts `@RequestBody CommentRequest(body: String, parentCommentId: Long?)`.
- Returns **201 Created** + `PostCommentResponse`.

##### 5.5 `GET /api/v1/family/{familyId}/posts/{postId}/comments`
- Returns **200 OK** + `List<PostCommentResponse>` (top-level with nested `replies`).

##### 5.6 `POST /api/v1/family/{familyId}/posts/{postId}/comments/{commentId}/heart`
- No request body. Reads `userId` from `X-User-Id`.
- Returns **200 OK** + `CommentHeartResponse(commentId, heartCount, hearted)`.

**Security note**: All endpoints fall under `/api/v1/family/{familyId}/**` and are automatically guarded by `FamilyMembershipInterceptor`. Non-members are blocked with 403 before any service logic runs.

#### Step 6: Create DTOs

| DTO | Fields |
|---|---|
| `FamilyPostResponse` | `postId`, `familyId`, `userId`, `contentText?`, `imageUrl?`, `createdAt`, `heartCount: Long`, `commentCount: Long`, `hearted: Boolean` |
| `HeartResponse` | `postId`, `heartCount: Long`, `hearted: Boolean` |
| `CommentRequest` | `body: String` (@NotBlank, max 1000 chars), `parentCommentId: Long?` |
| `PostCommentResponse` | `commentId`, `postId`, `userId`, `parentCommentId?`, `body`, `createdAt`, `heartCount: Long`, `hearted: Boolean`, `replies: List<PostCommentResponse>` |
| `CommentHeartResponse` | `commentId`, `heartCount: Long`, `hearted: Boolean` |
| `PagedResponse<T>` | `content: List<T>`, `page`, `size`, `totalElements`, `totalPages` |

#### Step 7: Add New Exception

`PostNotFoundException` → 404 Not Found (thrown when `{postId}` does not belong to `{familyId}`).

Register in `GlobalExceptionHandler`.

#### Step 8: Configure File Upload in `application.yml`

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 12MB

app:
  upload-dir: /uploads/images
```

Create the upload directory on the server:
```bash
sudo mkdir -p /uploads/images
sudo chown nakpom:nakpom /uploads/images
```

---

## Part B: Frontend Engineering Tasks (Android — separate repo)

> **Note**: Frontend is implemented in the separate Android repository. The section below is included as a reference specification for the Android team.

### Phase 1: Data Models & State

#### Step 9: Define Network Models

```kotlin
data class FamilyPostDto(
    val postId: Long,
    val userId: Int,
    val contentText: String?,
    val imageUrl: String?,
    val createdAt: String,
    val heartCount: Long,
    val commentCount: Long,
    val hearted: Boolean
)

data class PostCommentDto(
    val commentId: Long,
    val postId: Long,
    val userId: Int,
    val parentCommentId: Long?,
    val body: String,
    val createdAt: String,
    val heartCount: Long,
    val hearted: Boolean,
    val replies: List<PostCommentDto>   // empty list for replies themselves
)

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
```

#### Step 10: Define UI State

```kotlin
sealed interface FeedUiState {
    object Idle : FeedUiState
    object Loading : FeedUiState
    data class Success(val posts: List<FamilyPostDto>) : FeedUiState
    data class Error(val message: String) : FeedUiState
}
```

#### Step 11: FeedViewModel

- Exposes `uiState: StateFlow<FeedUiState>`
- `loadFeed(familyId, page)` → `GET /family/{familyId}/posts`
- `createPost(familyId, text?, imageUri?)` → multipart `POST /family/{familyId}/posts`
- `toggleHeart(familyId, postId)` → `POST /posts/{postId}/heart`; optimistic `heartCount`/`hearted` update
- `loadComments(familyId, postId)` → `GET /posts/{postId}/comments`
- `addComment(familyId, postId, body, parentCommentId?)` → `POST /posts/{postId}/comments`
- `toggleCommentHeart(familyId, postId, commentId)` → `POST /comments/{commentId}/heart`; optimistic update on the matching `PostCommentDto`
- On `createPost` success → refreshes feed from page 0

---

### Phase 2: UI Screens

#### Step 12: `FamilyFeedScreen.kt` — Interactive Timeline

```
[ Dashboard Top Bar ]  ← Toggle: Family Feed | Public Board
          │
          ▼
┌──────────────────────────────────────────────────────┐
│  🧑 Saoly Thang  •  2 mins ago                       │
│  "Testing the new Krousa Me space!"                  │
│  ┌────────────────────────────────────────────────┐  │
│  │              [ Post Image ]                    │  │
│  └────────────────────────────────────────────────┘  │
│  ❤️ 12   💬 3 comments                              │  ← tap ❤️ to toggle heart
└──────────────────────────────────────────────────────┘
          │
          ▼
[ Floating Action Button (+) ] → Opens NewPostModal
```

> No share button. Tapping 💬 opens a `CommentSheetScreen` for that post.

**Comment Sheet layout** (opens as `ModalBottomSheet`):
```
┌──────────────────────────────────────────────────────┐
│  💬 Comments                                  [Close] │
├──────────────────────────────────────────────────────┤
│  🧑 Mum  •  5m ago                                   │
│  "So cute!"                      ❤️ 3   [Reply]      │
│    ↳ 🧑 Saoly  •  3m ago                             │
│      "Thanks mum ❤️"             ❤️ 1   [Reply]      │
│  🧑 Dad  •  1m ago                                   │
│  "Love this!"                    ❤️ 0   [Reply]      │
├──────────────────────────────────────────────────────┤
│  [ Reply to: Mum ✕ ]  ← shown when replying         │
│  ┌──────────────────────────────┐  [Send]            │
│  │ Add a comment...             │                    │
│  └──────────────────────────────┘                    │
└──────────────────────────────────────────────────────┘
```

**Component structure**:
- `LazyColumn` renders each `FamilyPostDto` as a `PostCard` composable.
- `PostCard` action bar: ❤️ heart + 💬 comment count. **No share button.**
- Heart & comment buttons: minimum **56dp × 56dp** tap target.
- Tapping 💬 opens `CommentSheetScreen` (ModalBottomSheet).
- `CommentSheetScreen` renders top-level `PostCommentDto` items; each has an inline `replies` sub-list.
- Each comment row: avatar, body, timestamp, ❤️ count (tap to toggle), `[Reply]` button.
- Tapping `[Reply]` sets `replyTarget` state → prepends "Reply to: {username}" chip above the input field and sets `parentCommentId` in the next send.
- Loading: `CircularProgressIndicator` overlaid on screen.
- Error: `Snackbar` with retry action.

#### Step 13: `NewPostModal.kt` — Full-Screen Post Composer

- `ModalBottomSheet` or full-screen `Scaffold`.
- `OutlinedTextField` for text content.
- Image picker using `ActivityResultContracts.PickVisualMedia()`:
  ```kotlin
  val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
      viewModel.updateSelectedImageUri(uri)
  }
  ```
- Preview selected image with `AsyncImage`.
- Submit button → calls `FeedViewModel.createPost()`.
- Shows `CircularProgressIndicator` during upload.
- Closes modal and refreshes feed on success.

---

## Part C: Documentation & Verification

### Step 14: Update UML Diagrams
Extend `docs/uml-diagrams.md` with Sprint 3 additions:
- **Sequence Diagram**: `POST /family/{familyId}/posts` — multipart upload flow.
- **Sequence Diagram**: `GET /family/{familyId}/posts` — paginated feed fetch.
- **Sequence Diagram**: Reaction toggle (insert / update / delete logic).

### Step 15: Update Database Documentation
Extend `docs/database-schema.md`:
- Add Section 9 for Sprint 3: `family_posts` and `post_reactions` table specs.
- Document V3 migration.
- Document `idx_posts_family_created` composite index rationale.

### Step 16: Verify Backend Endpoints

#### 16.1 Create a Post (text only)
```bash
curl -X POST http://localhost:8080/api/v1/family/1/posts \
  -H "X-User-Id: 1" \
  -F "contentText=Hello from the family feed!"
```
Expected: `201 Created` + `FamilyPostResponse`

#### 16.2 Create a Post (with image)
```bash
curl -X POST http://localhost:8080/api/v1/family/1/posts \
  -H "X-User-Id: 1" \
  -F "contentText=Look at this!" \
  -F "image=@/path/to/photo.jpg"
```
Expected: `201 Created` + `imageUrl` field populated

#### 16.3 Fetch Feed (paginated)
```bash
curl "http://localhost:8080/api/v1/family/1/posts?page=0&size=20" \
  -H "X-User-Id: 1"
```
Expected: `200 OK` + paginated post list

#### 16.4 Toggle Heart on a Post
```bash
curl -X POST http://localhost:8080/api/v1/family/1/posts/1/heart \
  -H "X-User-Id: 1"
```
Expected: `200 OK` + `{"postId":1,"heartCount":1,"hearted":true}`. Calling again → `hearted: false`, count decrements.

#### 16.5a Add a Top-Level Comment
```bash
curl -X POST http://localhost:8080/api/v1/family/1/posts/1/comments \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"body":"Nice photo!"}'
```
Expected: `201 Created` + `PostCommentResponse` (parentCommentId: null).

#### 16.5b Reply to a Comment
```bash
curl -X POST http://localhost:8080/api/v1/family/1/posts/1/comments \
  -H "X-User-Id: 2" \
  -H "Content-Type: application/json" \
  -d '{"body":"Thanks!","parentCommentId":1}'
```
Expected: `201 Created` + `PostCommentResponse` (parentCommentId: 1).

#### 16.5c Fetch Comments (with replies)
```bash
curl http://localhost:8080/api/v1/family/1/posts/1/comments \
  -H "X-User-Id: 1"
```
Expected: `200 OK` + top-level comments, each with nested `replies` array.

#### 16.5d Toggle Heart on a Comment
```bash
curl -X POST http://localhost:8080/api/v1/family/1/posts/1/comments/1/heart \
  -H "X-User-Id: 2"
```
Expected: `200 OK` + `{"commentId":1,"heartCount":1,"hearted":true}`. Calling again → decrements.

#### 16.6 Test Interceptor Still Guards Feed
```bash
curl http://localhost:8080/api/v1/family/1/posts \
  -H "X-User-Id: 99"
```
Expected: `403 Forbidden`

#### 16.6 Verify Database State
```sql
SELECT * FROM family_posts    ORDER BY created_at DESC LIMIT 10;
SELECT * FROM post_reactions  WHERE post_id = 1;
```

---

## Runtime Troubleshooting Notes

### Multipart upload fails with 413
**Symptom**: `MaxUploadSizeExceededException`

**Fix**: Ensure `spring.servlet.multipart.max-file-size` and `max-request-size` are set in `application.yml`. Also check your reverse proxy (if any) has a matching body size limit.

### Images not found after upload
**Symptom**: `imageUrl` returns a path but the file is not accessible.

**Fix**: Confirm the upload directory exists and is writable by the process user. Map the directory as a static resource or serve via a dedicated endpoint:
```kotlin
// In WebConfig.kt
override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:/uploads/")
}
```

### Reaction toggle not working idempotently
**Symptom**: Multiple rows inserted for the same user/post.

**Fix**: Composite PK `(post_id, user_id)` in the DB prevents duplicates at the database level — if you see this error, the application is catching a `DataIntegrityViolationException` instead of using the repository check. Always call `findByPostIdAndUserId()` before `save()`.

### Feed returns posts from wrong family
**Symptom**: User in family 2 can see family 1 posts.

**Fix**: The interceptor checks membership but the repository query must also filter by `familyId`. Verify `findByFamilyIdOrderByCreatedAtDesc(familyId, pageable)` — the `familyId` parameter must come from the path variable, not from the request body.

---

## Success Criteria

### Backend Track
- [ ] V3 Flyway migration — `family_posts`, `post_reactions`, `post_comments`, `comment_reactions` tables
- [ ] `FamilyPost`, `PostReaction`, `PostComment`, `CommentReaction` JPA entities
- [ ] `FamilyPostRepository` with paginated query
- [ ] `PostReactionRepository` — post heart toggle
- [ ] `PostCommentRepository` — top-level + reply queries
- [ ] `CommentReactionRepository` — comment heart toggle
- [ ] `PostService.createPost()` — text/image validation, local file write, DB save
- [ ] `PostService.getFeed()` — paginated, reverse-chronological, heartCount + commentCount
- [ ] `PostService.toggleHeart()` — idempotent heart/un-heart on post
- [ ] `PostService.addComment(parentCommentId?)` — top-level & replies
- [ ] `PostService.getComments()` — nested reply tree with heartCount per comment
- [ ] `PostService.toggleCommentHeart()` — idempotent heart/un-heart on comment
- [ ] `POST /api/v1/family/{familyId}/posts` → 201
- [ ] `GET /api/v1/family/{familyId}/posts?page=0&size=20` → 200
- [ ] `POST /api/v1/family/{familyId}/posts/{postId}/heart` → 200
- [ ] `POST /api/v1/family/{familyId}/posts/{postId}/comments` → 201 (supports replies via `parentCommentId`)
- [ ] `GET /api/v1/family/{familyId}/posts/{postId}/comments` → 200 (nested)
- [ ] `POST /api/v1/family/{familyId}/posts/{postId}/comments/{commentId}/heart` → 200
- [ ] `PostNotFoundException`, `CommentNotFoundException` in `GlobalExceptionHandler`
- [ ] File upload configured in `application.yml`
- [ ] All endpoints return 403 for non-members

### Frontend Track (separate Android repo)
- [ ] `FamilyPostDto`, `PostCommentDto` (with `replies`, `heartCount`, `hearted`) network models
- [ ] `FeedUiState` sealed interface
- [ ] `FeedViewModel`: `loadFeed()`, `createPost()`, `toggleHeart()`, `loadComments()`, `addComment(parentCommentId?)`, `toggleCommentHeart()`
- [ ] `FamilyFeedScreen` — `LazyColumn` with `PostCard` composables; no share button
- [ ] Heart & comment buttons ≥ 56dp × 56dp
- [ ] Post heart animates (fill ↔ outline) with optimistic update
- [ ] Comment sheet opens in `ModalBottomSheet` on 💬 tap
- [ ] Comment sheet shows threaded replies (one level deep)
- [ ] Each comment row has ❤️ toggle + `[Reply]` button
- [ ] Tapping `[Reply]` prefills `parentCommentId` and shows reply chip
- [ ] Comment heart animates with optimistic update
- [ ] `NewPostModal` — text field, image picker, upload progress
- [ ] Multipart request wired to `POST /posts`
- [ ] Feed auto-refreshes after post creation
