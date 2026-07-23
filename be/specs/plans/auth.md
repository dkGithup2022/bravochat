# Plan: Auth 도메인 (POST /auth/login + DELETE /auth/session)

> be-planner 배치 1. **prnd 레퍼런스 Auth 수직 슬라이스를 미러링** + 결정사항 반영.
> Case A(신규 도메인). 로그아웃은 레퍼런스에 없어 레퍼런스 스타일로 신규 설계.

## 확정된 결정 (F1~F9)
- F1 soft-delete: **전 테이블 is_deleted/deleted_at 추가** (레퍼런스 동일)
- F2 PK: **전부 `id`** (@Id Long id, 컬럼 id) — 레퍼런스 동일
- F3 시각: **Java는 Instant로 통일**(LocalDateTime 안 씀), DDL은 DATETIME(6) (레퍼런스 동일). API 응답 문자열화는 api 레이어.
- F4 세션 없음/만료 → `InvalidSessionException(401)`
- F5 로그아웃: userId/소유권 없이 **sessionKey만 읽어 revoke(soft-delete)**, 멱등
- F6 비번검증: `PasswordVerifier` + `BcryptPasswordVerifier`(spring-security-crypto) — 레퍼런스 동일
- F7 세션 TTL: **7일** (레퍼런스 3일 → 7일), key=UUID
- F8 Read 모델 미분리 (도메인 모델 직접)
- F9 Identity VO 유지: `UserIdentity`, `LoginSessionIdentity`

## chat 스펙 대비 의도적 편차 (레퍼런스 미러링 결과)
1. `User`에 **status(UserStatus) 없음** → `UserDisabledException` 없음 (레퍼런스 User엔 상태 없음)
2. `LoginSession`: status/expiresAt/lastAccessedAt 대신 **lastLoggedInAt/lastRequestedAt + 계산 만료**(lastLoggedInAt + 7일)
3. `LoginResult` = **sessionKey만** (expiresAt 없음 — API가 204+헤더라 불필요)
4. 로그아웃 = **soft-delete** (REVOKED 상태 개념 없음), 멱등
5. password 컬럼은 **`password_hash`** (결정2, 레퍼런스는 `password`였음 — 유일한 컬럼명 편차). 엔티티 필드 `passwordHash` ↔ 도메인 `password`

---

## 구성요소 (레퍼런스 미러, 패키지 com.chatbot.bravo)

### model (model 모듈, .model.auth)
- `User`(@Value, AuditFields): userId, username, password, Instant createdAt, updatedAt. `create(username, password)`
- `UserIdentity`(@Value): userId
- `LoginSession`(@Value, AuditFields): loginSessionId, sessionKey, userId, lastLoggedInAt, lastRequestedAt, createdAt, updatedAt. `issue(userId)`(UUID key, TTL 7일 계산은 isExpired에서), `touch()`, `isExpired(now)` = now > lastLoggedInAt+7일
- `LoginSessionIdentity`(@Value): loginSessionId

### exception (exception 모듈, .exception.auth)
- `LoginFailedException`(401, "아이디 또는 비밀번호가 올바르지 않습니다") — 계정없음/비번불일치 미구분
- `InvalidSessionException`(401, "세션이 유효하지 않습니다...")

### infrastructure (infrastructure 모듈, .infrastructure.auth.repository) — 포트
- `UserRepository`: `Optional<User> findByUsername(String)` (배치1엔 findById/save 불필요 — 유저는 data.sql 시드)
- `LoginSessionRepository`: `LoginSession save(LoginSession)`, `Optional<LoginSession> findBySessionKey(String)`, `void deleteBySessionKey(String)`(신규, 로그아웃 soft-delete)

### repository-jdbc (.jdbc.auth.repository)
- `UserEntity`(@Table("users"), @Id Long id, username, passwordHash, isDeleted, deletedAt, @CreatedDate createdAt, @LastModifiedDate updatedAt) + toDomain/from/softDelete
- `UserEntityRepository` extends CrudRepository: `findByUsernameAndIsDeletedFalse`
- `UserJdbcRepository` implements UserRepository
- `LoginSessionEntity`(@Table("login_sessions"), 필드 미러) + toDomain/from/softDelete
- `LoginSessionEntityRepository`: `findBySessionKeyAndIsDeletedFalse`
- `LoginSessionJdbcRepository`: save, findBySessionKey, deleteBySessionKey(→ 조회 후 softDelete save, 멱등)

### service (.service.auth) — usecase는 @Service 구체 클래스
- `LoginUsecase`(@Service): login(LoginCommand)→LoginResult. findByUsername→없으면 LoginFailed→passwordVerifier.matches 실패 시 LoginFailed→sessionManager.newOne(userId)→LoginResult(sessionKey)
- `LogoutUsecase`(@Service, 신규): logout(LogoutCommand)→void. sessionManager.revoke(sessionKey)
- `PasswordVerifier`(interface) + impl/`BcryptPasswordVerifier`(@Component, BCryptPasswordEncoder)
- `SessionManager`(interface): `newOne(userId)`, `check(sessionKey)`, `revoke(sessionKey)`(신규) + impl/`DefaultSessionManager`(@Service, @Transactional)
- dto(record): `LoginCommand`(username, rawPassword), `LoginResult`(sessionKey), `LogoutCommand`(sessionKey)

### api (.api.auth)
- `AuthApiController`: 
  - `POST /auth/login`(@Valid LoginRequest) → 204 + `Authorization: Bearer {sessionKey}`
  - `DELETE /auth/session`(@RequestHeader Authorization) → "Bearer " 파싱→sessionKey, 형식오류 시 InvalidSessionException(401) → logout → 204
- dto/`LoginRequest`(username, password, @NotBlank) + toCommand()

---

## DDL (schema.sql append, FK 없음, DATETIME(6))
```sql
-- === Auth ===
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    DATETIME(6),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
);
CREATE TABLE IF NOT EXISTS login_sessions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_key       VARCHAR(36)  NOT NULL,
    user_id           BIGINT       NOT NULL,
    last_logged_in_at DATETIME(6)  NOT NULL,
    last_requested_at DATETIME(6)  NOT NULL,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at        DATETIME(6),
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    CONSTRAINT uk_login_sessions_session_key UNIQUE (session_key)
);
CREATE INDEX IF NOT EXISTS idx_login_sessions_user_id ON login_sessions(user_id);
```

## 시드 (data.sql) — 로그인 검증용 테스트 유저
- username=`tester`, password_hash=BCrypt("password1234") (구현 시 실제 해시 생성해 삽입)

## 구현 순서 (bottom-up)
1. add-model: User, UserIdentity, LoginSession, LoginSessionIdentity
2. add-infrastructure: UserRepository, LoginSessionRepository
3. add-entity-specs: UserEntity, LoginSessionEntity + DDL append + data.sql 시드
4. add-jdbc-query: EntityRepository/JdbcRepository 4종
5. add-usecase: PasswordVerifier(+Bcrypt), SessionManager(+Default), LoginUsecase, LogoutUsecase, DTO, 예외 2종. service build.gradle에 spring-security-crypto
6. add-api: AuthApiController, LoginRequest
7. 검증: compile + (be-test로) 단위/통합 테스트, bootRun 스모크

---

## Phase 3: 구현 진행 (완료 2026-07-23)

전 레이어 구현 완료. compile green, bootRun e2e 검증 통과.

**e2e (시드 tester/password1234, local 프로파일):**
| 시나리오 | 결과 |
|---|---|
| 로그인 성공 → 204 + Authorization: Bearer {key} | ✅ |
| 로그인 실패(틀린 비번) → 401 | ✅ |
| 로그인 검증(username 누락) → 400 | ✅ |
| 로그아웃(발급 키) → 204 | ✅ |
| 로그아웃 재호출(멱등) → 204 | ✅ |
| 로그아웃 헤더 없음 → 401 | ✅ |

**DB 검증:** users 시드 존재(BCrypt), 로그아웃된 세션 is_deleted=TRUE + deleted_at 세팅(soft-delete), 미로그아웃 세션 유지.

**상태 문서 갱신:** specs/api.md, usecase.md, infrastructure.md, models.md 에 Auth 반영.

**미작성:** be-test 단위/통합 테스트 (게이트 B에서 진행 여부 결정).

