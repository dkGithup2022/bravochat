# Chat Use Case & DTO Specification

## 1. 공개 유즈케이스 목록

외부 계층에서 호출할 수 있는 유즈케이스는 아래 4개로 제한합니다.

```text
LoginUseCase
LogoutUseCase
SendMessageUseCase
GetRecentTurnsUseCase
```

LLM 메시지 구성, 툴 호출, 툴 결과 append, 반복 실행, TurnEvent 저장은 `SendMessageUseCase` 내부 구현으로 숨깁니다.

---

## 2. LoginUseCase

사용자 인증 후 로그인 세션을 발급합니다.

### Interface

```text
login(LoginCommand) -> LoginResult
```

### LoginCommand

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `username` | `String` | O | 로그인 아이디 |
| `rawPassword` | `String` | O | 사용자가 입력한 평문 비밀번호 |

### LoginResult

| 필드 | 타입 | 설명 |
|---|---|---|
| `sessionKey` | `String` | 발급된 세션 키 |
| `expiresAt` | `LocalDateTime` | 세션 만료 시각 |

### 처리 규칙

1. 사용자 조회
2. 비밀번호 검증
3. 로그인 세션 생성
4. 세션 키 반환

### 실패

| 예외 | 조건 |
|---|---|
| `LoginFailedException` | 사용자가 없거나 비밀번호가 일치하지 않음 |
| `UserDisabledException` | 비활성화된 사용자 |
| `SessionIssueFailedException` | 세션 발급 실패 |

---

## 3. LogoutUseCase

현재 로그인 세션을 만료합니다.

### Interface

```text
logout(LogoutCommand) -> void
```

### LogoutCommand

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `userId` | `Long` | O | 인증된 사용자 식별자 |
| `sessionKey` | `String` | O | 만료할 세션 키 |

### 처리 규칙

- 세션 소유자가 `userId`와 일치해야 합니다.
- 세션 상태를 `REVOKED`로 변경하거나 삭제합니다.
- 이미 만료된 세션에 대한 처리 방식은 멱등하게 성공으로 간주할 수 있습니다.

### 실패

| 예외 | 조건 |
|---|---|
| `SessionNotFoundException` | 대상 세션이 존재하지 않음 |
| `SessionOwnershipException` | 세션 소유자가 일치하지 않음 |

---

## 4. SendMessageUseCase

사용자 메시지를 받아 LLM 및 툴 실행 루프를 수행하고 최종 응답을 반환합니다.

### Interface

```text
sendMessage(SendMessageCommand) -> SendMessageResult
```

### SendMessageCommand

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `userId` | `Long` | O | 인증된 사용자 식별자 |
| `message` | `String` | O | 사용자 입력 메시지 |

### SendMessageResult

| 필드 | 타입 | 설명 |
|---|---|---|
| `turnId` | `Long` | 생성된 Turn 식별자 |
| `message` | `String` | 사용자에게 반환할 최종 응답 |
| `createdAt` | `LocalDateTime` | 최종 응답 생성 시각 |

### 처리 흐름

```text
Turn 생성
→ USER_MESSAGE 저장
→ LLM 요청 메시지 구성
→ LLM 호출
→ TOOL_CALL이면 툴 실행
→ TOOL_RESULT 저장 및 컨텍스트 append
→ LLM 재호출
→ ASSISTANT_MESSAGE 저장
→ Turn 완료
→ 최종 응답 반환
```

### 내부 처리 기준

- 일반 응답은 `USER_MESSAGE → ASSISTANT_MESSAGE`로 종료합니다.
- 툴 호출이 발생하면 `TOOL_CALL → TOOL_RESULT`를 순서대로 append합니다.
- 툴 호출은 0회 이상 반복될 수 있습니다.
- 외부 호출자에게는 최종 응답만 반환합니다.
- TurnEvent, LLM 컨텍스트, 툴 실행 구조는 공개 DTO에 노출하지 않습니다.

### 실패

| 예외 | 조건 |
|---|---|
| `InvalidMessageException` | 메시지가 비어 있거나 허용 길이를 초과함 |
| `TurnCreationFailedException` | Turn 생성 실패 |
| `LlmExecutionException` | LLM 호출 실패 |
| `ToolExecutionException` | 툴 실행 실패 |
| `TurnCompletionFailedException` | 최종 응답 또는 Turn 완료 저장 실패 |

---

## 5. GetRecentTurnsUseCase

현재 사용자의 최근 대화를 최대 20개 조회합니다.

### Interface

```text
getRecentTurns(GetRecentTurnsQuery) -> GetRecentTurnsResult
```

### GetRecentTurnsQuery

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `userId` | `Long` | O | 인증된 사용자 식별자 |
| `size` | `Integer` | O | 조회 개수, 최대 20 |

### GetRecentTurnsResult

| 필드 | 타입 | 설명 |
|---|---|---|
| `turns` | `List<RecentTurnDto>` | 최근 대화 목록 |

### RecentTurnDto

| 필드 | 타입 | 설명 |
|---|---|---|
| `turnId` | `Long` | Turn 식별자 |
| `userMessage` | `String` | 사용자 입력 |
| `assistantMessage` | `String` | 최종 응답 |
| `createdAt` | `LocalDateTime` | Turn 생성 시각 |

### 조회 규칙

- 인증된 사용자의 Turn만 조회합니다.
- 최근 생성된 Turn을 기준으로 최대 20개 조회합니다.
- 응답 목록은 화면 출력에 맞게 오래된 순에서 최신 순으로 정렬합니다.
- `TOOL_CALL`, `TOOL_RESULT`는 응답에 포함하지 않습니다.
- 완료된 Turn만 조회하는 것을 기본으로 합니다.
- 조회 결과가 없으면 빈 목록을 반환합니다.

### 실패

| 예외 | 조건 |
|---|---|
| `InvalidRecentTurnSizeException` | 조회 크기가 1 미만이거나 20을 초과함 |
| `RecentTurnsLoadFailedException` | 최근 대화 조회 실패 |

### TODO

- 커서 기반 페이지네이션
- `cursor`, `nextCursor`, `hasNext` 필드 추가
- 커서 후보: `turnId` 또는 `(createdAt, turnId)`

---

## 6. DTO 분류

### Command

상태를 변경하는 유즈케이스 입력입니다.

```text
LoginCommand
LogoutCommand
SendMessageCommand
```

### Query

데이터를 조회하는 유즈케이스 입력입니다.

```text
GetRecentTurnsQuery
```

### Result

유즈케이스 실행 결과입니다.

```text
LoginResult
SendMessageResult
GetRecentTurnsResult
```

### Read DTO

조회 결과에 포함되는 읽기 전용 데이터입니다.

```text
RecentTurnDto
```

---

## 7. 계층 간 전달 규칙

- API Request DTO는 application Command 또는 Query로 변환합니다.
- API Response DTO는 application Result를 기반으로 생성합니다.
- application DTO는 HTTP 헤더, 상태 코드, JSON 구조를 알지 않습니다.
- `userId`와 `sessionKey`는 인증 계층에서 검증된 값만 전달합니다.
- 도메인 모델과 application DTO를 동일 객체로 사용하지 않습니다.
- 내부 실행 모델인 `TurnEvent`, `ToolCall`, `ToolResult`는 공개 유즈케이스 DTO에 노출하지 않습니다.
