# Chat RDBMS & Repository Interface Specification

## 1. 테이블 구성

```text
users
login_sessions
turns
turn_events
```

관계는 다음과 같습니다.

```text
users 1 ─── N login_sessions
users 1 ─── N turns
turns 1 ─── N turn_events
```

---

## 2. users

사용자 계정 정보를 저장합니다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `user_id` | `BIGINT` | PK | 사용자 식별자 |
| `username` | `VARCHAR(100)` | NOT NULL, UNIQUE | 로그인 아이디 |
| `password_hash` | `VARCHAR(255)` | NOT NULL | 암호화된 비밀번호 |
| `status` | `VARCHAR(20)` | NOT NULL | 사용자 상태 |
| `created_at` | `TIMESTAMP` | NOT NULL | 생성 시각 |
| `updated_at` | `TIMESTAMP` | NOT NULL | 수정 시각 |

### UserStatus

```text
ACTIVE
DISABLED
```

### 인덱스

```text
UNIQUE(username)
```

---

## 3. login_sessions

로그인 성공 후 발급되는 인증 세션을 저장합니다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `session_id` | `BIGINT` | PK | 세션 식별자 |
| `session_key` | `VARCHAR(255)` | NOT NULL, UNIQUE | 인증 세션 키 |
| `user_id` | `BIGINT` | NOT NULL, FK | 세션 소유 사용자 |
| `status` | `VARCHAR(20)` | NOT NULL | 세션 상태 |
| `created_at` | `TIMESTAMP` | NOT NULL | 발급 시각 |
| `expires_at` | `TIMESTAMP` | NOT NULL | 만료 시각 |
| `last_accessed_at` | `TIMESTAMP` | NULL | 마지막 사용 시각 |

### SessionStatus

```text
ACTIVE
EXPIRED
REVOKED
```

### 인덱스

```text
UNIQUE(session_key)
INDEX(user_id)
INDEX(status, expires_at)
```

### 외래 키

```text
login_sessions.user_id
→ users.user_id
```

---

## 4. turns

사용자 요청 1회부터 최종 응답까지의 실행 단위를 저장합니다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `turn_id` | `BIGINT` | PK | Turn 식별자 |
| `user_id` | `BIGINT` | NOT NULL, FK | 요청 사용자 |
| `status` | `VARCHAR(20)` | NOT NULL | 처리 상태 |
| `created_at` | `TIMESTAMP` | NOT NULL | 처리 시작 시각 |
| `completed_at` | `TIMESTAMP` | NULL | 처리 완료 시각 |
| `failure_reason` | `TEXT` | NULL | 내부 실패 정보 |

### TurnStatus

```text
PROCESSING
COMPLETED
FAILED
```

### 인덱스

```text
INDEX(user_id, created_at DESC, turn_id DESC)
INDEX(status)
```

최근 대화 조회는 `user_id`, `created_at`, `turn_id` 복합 인덱스를 사용합니다.

### 외래 키

```text
turns.user_id
→ users.user_id
```

---

## 5. turn_events

Turn 내부에서 발생한 메시지와 툴 실행 기록을 순서대로 저장합니다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `event_id` | `BIGINT` | PK | 이벤트 식별자 |
| `turn_id` | `BIGINT` | NOT NULL, FK | 소속 Turn |
| `sequence` | `INTEGER` | NOT NULL | Turn 내부 실행 순서 |
| `type` | `VARCHAR(30)` | NOT NULL | 이벤트 유형 |
| `content` | `LONGTEXT` | NOT NULL | 메시지, 툴 인자 또는 툴 결과 |
| `tool_name` | `VARCHAR(100)` | NULL | 호출한 툴 이름 |
| `tool_call_id` | `VARCHAR(100)` | NULL | 툴 호출 연결 식별자 |
| `created_at` | `TIMESTAMP` | NOT NULL | 이벤트 생성 시각 |

### TurnEventType

```text
USER_MESSAGE
TOOL_CALL
TOOL_RESULT
ASSISTANT_MESSAGE
```

### 필드 사용 기준

- `USER_MESSAGE`
  - `content`에 사용자 입력을 저장합니다.
  - `tool_name`, `tool_call_id`는 `NULL`입니다.

- `TOOL_CALL`
  - `tool_name`에 호출할 툴 이름을 저장합니다.
  - `tool_call_id`에 호출 식별자를 저장합니다.
  - `content`에 툴 호출 인자를 JSON 형식으로 저장합니다.

- `TOOL_RESULT`
  - 대응하는 `TOOL_CALL`과 동일한 `tool_call_id`를 사용합니다.
  - `content`에 툴 실행 결과를 JSON 형식으로 저장합니다.

- `ASSISTANT_MESSAGE`
  - `content`에 사용자에게 반환한 최종 응답을 저장합니다.
  - `tool_name`, `tool_call_id`는 `NULL`입니다.

### 인덱스 및 제약

```text
UNIQUE(turn_id, sequence)
INDEX(turn_id, sequence)
INDEX(tool_call_id)
```

### 외래 키

```text
turn_events.turn_id
→ turns.turn_id
```

---

## 6. 저장 규칙

- Turn을 먼저 `PROCESSING` 상태로 생성합니다.
- `TurnEvent`는 `sequence` 순서대로 append합니다.
- 기존 이벤트는 수정하지 않는 것을 기본으로 합니다.
- 최종 응답 저장 후 Turn 상태를 `COMPLETED`로 변경합니다.
- 처리 중 오류가 발생하면 Turn 상태를 `FAILED`로 변경하고 `failure_reason`을 저장합니다.
- 최근 대화 조회에서는 `COMPLETED` 상태의 Turn만 기본 조회 대상으로 사용합니다.

---

## 7. UserRepository

사용자 계정 저장과 조회를 담당합니다.

### Interface

```text
findByUsername(username) -> Optional<User>
findById(userId) -> Optional<User>
save(user) -> User
```

### 사용 목적

- 로그인 사용자 조회
- 사용자 상태 확인
- 사용자 생성 또는 변경

---

## 8. LoginSessionRepository

로그인 세션 저장과 조회를 담당합니다.

### Interface

```text
save(session) -> LoginSession
findBySessionKey(sessionKey) -> Optional<LoginSession>
```

### 사용 목적

- 로그인 세션 생성
- 요청 인증
- 로그아웃 처리
- 세션 만료 처리

### 로그아웃 처리

```text
findBySessionKey
→ 세션 상태를 REVOKED로 변경
→ save
```

---

## 9. TurnRepository

Turn의 생성과 상태 변경을 담당합니다.

### Interface

```text
save(turn) -> Turn
findById(turnId) -> Optional<Turn>
```

### 사용 목적

- Turn 최초 생성
- `PROCESSING → COMPLETED`
- `PROCESSING → FAILED`
- 완료 시각 저장
- 실패 사유 저장

---

## 10. TurnEventRepository

Turn 내부 이벤트를 순서대로 저장하고 조회합니다.

### Interface

```text
append(event) -> TurnEvent
appendAll(events) -> List<TurnEvent>
findAllByTurnIdOrderBySequence(turnId) -> List<TurnEvent>
```

### 저장 흐름

```text
USER_MESSAGE append
→ TOOL_CALL append
→ TOOL_RESULT append
→ ASSISTANT_MESSAGE append
```

툴 호출이 반복되면 기존 이벤트를 수정하지 않고 뒤에 계속 추가합니다.

---

## 11. RecentTurnQueryRepository

최근 대화 조회를 위한 읽기 전용 인터페이스입니다.

### Interface

```text
findRecentCompletedTurns(userId, size)
    -> List<RecentTurnData>
```

### RecentTurnData

| 필드 | 타입 | 설명 |
|---|---|---|
| `turnId` | `Long` | Turn 식별자 |
| `userMessage` | `String` | `USER_MESSAGE` 내용 |
| `assistantMessage` | `String` | `ASSISTANT_MESSAGE` 내용 |
| `createdAt` | `LocalDateTime` | Turn 생성 시각 |

### 조회 규칙

- 사용자별 완료된 Turn만 조회합니다.
- 최근 생성된 Turn을 기준으로 최대 `size`개 조회합니다.
- `USER_MESSAGE`, `ASSISTANT_MESSAGE` 이벤트를 조인하여 반환합니다.
- 최종 응답 목록은 오래된 순에서 최신 순으로 정렬합니다.

최근 대화 조회를 `TurnRepository`와 `TurnEventRepository` 조합으로 처리하면 추가 조회가 발생할 수 있으므로, 전용 Query Repository로 분리합니다.

---

## 12. 인터페이스 목록

```text
UserRepository
LoginSessionRepository
TurnRepository
TurnEventRepository
RecentTurnQueryRepository
```

---

## TODO

- 최근 대화 커서 기반 페이지네이션
- 커서 후보: `turnId` 또는 `(createdAt, turnId)`
- 커서 적용 후 인터페이스 예시

```text
findRecentCompletedTurns(
    userId,
    cursor,
    size
) -> RecentTurnSlice
```

- 오래된 세션 및 실패 Turn 정리 정책
- 툴 결과에 민감정보가 포함되는 경우 저장 및 마스킹 정책
