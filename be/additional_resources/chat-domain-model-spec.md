# Chat Domain Model Specification

## 1. User

로그인 가능한 사용자입니다.

| 필드 | 설명 |
|---|---|
| `userId` | 사용자 식별자 |
| `username` | 로그인 아이디 |
| `password` | 암호화된 비밀번호 |
| `status` | 사용자 상태 |
| `createdAt` | 생성 시각 |

### UserStatus

- `ACTIVE`
- `DISABLED`

---

## 2. LoginSession

로그인 성공 시 발급되는 인증 세션입니다.

| 필드 | 설명 |
|---|---|
| `sessionId` | 세션 식별자 |
| `sessionKey` | 클라이언트에 발급되는 인증 키 |
| `userId` | 세션 소유 사용자 |
| `status` | 세션 상태 |
| `createdAt` | 발급 시각 |
| `expiresAt` | 만료 시각 |
| `lastAccessedAt` | 마지막 사용 시각 |

### SessionStatus

- `ACTIVE`
- `EXPIRED`
- `REVOKED`

---

## 3. Turn

사용자 요청 1회부터 최종 응답까지의 전체 실행 단위입니다.

메시지 본문이나 툴 실행 내용은 직접 보관하지 않고, `TurnEvent`에 순서대로 저장합니다.

| 필드 | 설명 |
|---|---|
| `turnId` | Turn 식별자 |
| `userId` | 요청 사용자 |
| `status` | 처리 상태 |
| `createdAt` | 처리 시작 시각 |
| `completedAt` | 처리 완료 시각 |

### TurnStatus

- `PROCESSING`
- `COMPLETED`
- `FAILED`

---

## 4. TurnEvent

Turn 내부에서 발생한 메시지와 툴 실행 기록을 순서대로 저장합니다.

| 필드 | 설명 |
|---|---|
| `eventId` | 이벤트 식별자 |
| `turnId` | 소속 Turn |
| `sequence` | Turn 내부 실행 순서 |
| `type` | 이벤트 유형 |
| `content` | 메시지, 툴 인자 또는 툴 결과 |
| `toolName` | 호출한 툴 이름 |
| `toolCallId` | 툴 호출과 결과를 연결하는 식별자 |
| `createdAt` | 이벤트 생성 시각 |

### TurnEventType

| 타입 | 의미 | 사용자 노출 |
|---|---|---|
| `USER_MESSAGE` | 사용자 입력 | 노출 |
| `TOOL_CALL` | LLM이 생성한 툴 호출 요청 | 내부 |
| `TOOL_RESULT` | 툴 실행 결과 | 내부 |
| `ASSISTANT_MESSAGE` | 사용자에게 반환되는 최종 응답 | 노출 |

### 필드 사용 기준

- `USER_MESSAGE`
  - `content`에 사용자 입력을 저장합니다.
  - `toolName`, `toolCallId`는 사용하지 않습니다.

- `TOOL_CALL`
  - `toolName`에 호출할 툴 이름을 저장합니다.
  - `toolCallId`에 호출 식별자를 저장합니다.
  - `content`에 툴 호출 인자를 저장합니다.

- `TOOL_RESULT`
  - 대응하는 `TOOL_CALL`과 동일한 `toolCallId`를 사용합니다.
  - `content`에 툴 실행 결과를 저장합니다.

- `ASSISTANT_MESSAGE`
  - `content`에 최종 응답을 저장합니다.
  - `toolName`, `toolCallId`는 사용하지 않습니다.

---

## 5. 모델 관계

```text
User 1 ─── N LoginSession
User 1 ─── N Turn
Turn 1 ─── N TurnEvent
```

현재는 사용자별 대화방을 분리하지 않으므로 `Conversation` 모델은 두지 않습니다.

---

## 6. 실행 흐름

### 일반 응답

```text
Turn 생성
status = PROCESSING

1. USER_MESSAGE
2. ASSISTANT_MESSAGE

Turn 완료
status = COMPLETED
```

### 툴 호출 1회

```text
Turn 생성
status = PROCESSING

1. USER_MESSAGE
2. TOOL_CALL
3. TOOL_RESULT
4. ASSISTANT_MESSAGE

Turn 완료
status = COMPLETED
```

### 툴 호출 반복

```text
Turn 생성
status = PROCESSING

1. USER_MESSAGE
2. TOOL_CALL
3. TOOL_RESULT
4. TOOL_CALL
5. TOOL_RESULT
6. ASSISTANT_MESSAGE

Turn 완료
status = COMPLETED
```

각 이벤트는 `sequence` 순서대로 append합니다.

---

## 7. 처리 규칙

- 하나의 Turn에는 `USER_MESSAGE`가 정확히 하나 존재합니다.
- 하나의 완료된 Turn에는 `ASSISTANT_MESSAGE`가 정확히 하나 존재합니다.
- `TOOL_CALL`과 `TOOL_RESULT`는 동일한 `toolCallId`로 연결합니다.
- 한 Turn 안에서 `sequence` 값은 중복될 수 없습니다.
- 툴 호출이 여러 번 발생하면 기존 이벤트를 수정하지 않고 뒤에 계속 추가합니다.
- 최종 응답이 저장되면 Turn 상태를 `COMPLETED`로 변경합니다.
- 처리 중 오류가 발생하면 Turn 상태를 `FAILED`로 변경합니다.

---

## 8. 최근 대화 조회 기준

최근 대화 API에서는 사용자에게 노출되는 이벤트만 사용합니다.

- `USER_MESSAGE`
- `ASSISTANT_MESSAGE`

`TOOL_CALL`, `TOOL_RESULT`는 내부 실행 기록이므로 최근 대화 응답에 포함하지 않습니다.

---

## TODO

- 대화방 분리가 필요해질 경우 `Conversation` 모델 추가
- 과거 대화 추가 로딩을 위한 커서 기반 페이지네이션
- 민감한 툴 입력 및 결과 저장 정책
- 실패한 툴 호출의 재시도 횟수 및 상세 상태 모델
