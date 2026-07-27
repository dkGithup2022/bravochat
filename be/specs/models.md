# Domain Models (현재 상태)

> add-* 스킬이 도메인 추가 시 이 문서를 갱신한다. assess-model이 참조.

## auth (com.chatbot.bravo.model.auth)

### User (@Value, AuditFields)
- 필드: `Long userId`, `String username`, `String password`(인코딩된 해시), `Instant createdAt`, `Instant updatedAt`
- 팩토리: `create(username, password)`

### UserIdentity (@Value)
- `Long userId`

### LoginSession (@Value, AuditFields)
- 필드: `Long loginSessionId`, `String sessionKey`, `Long userId`, `Instant lastLoggedInAt`, `Instant lastRequestedAt`, `Instant createdAt`, `Instant updatedAt`
- 상수: SESSION_TTL = 7일
- 팩토리: `issue(userId)` (UUID key)
- 상태변경: `touch()` (lastRequestedAt 갱신, 새 인스턴스)
- 판정: `isExpired(Instant now)` = now > lastLoggedInAt + 7일

### LoginSessionIdentity (@Value)
- `Long loginSessionId`

> 규약: soft delete 필드(isDeleted/deletedAt)는 모델에 없음 — Entity 레이어에서만 관리. status enum 미사용(레퍼런스 미러링).

## chat (com.chatbot.bravo.model.chat)

### RecentTurn (@Value) — 읽기 프로젝션
- 필드: `Long turnId`, `String userMessage`, `String assistantMessage`, `Instant createdAt`
- 한 Turn의 USER_MESSAGE + 최종 ASSISTANT_MESSAGE agg 결과 (TOOL_* 제외). 도메인 엔티티 아님.

### Turn (@Value, AuditFields)
- 필드: `Long turnId`, `Long userId`, `TurnStatus status`, `Instant completedAt`(nullable), `String failureReason`(nullable), createdAt, updatedAt
- 팩토리/상태변경: `start(userId)`→PROCESSING, `complete()`→COMPLETED+completedAt, `fail(reason)`→FAILED+completedAt+failureReason
- `TurnIdentity`(turnId), `TurnStatus` enum: PROCESSING/COMPLETED/FAILED

### TurnEvent (@Value, AuditFields)
- 필드: `Long eventId`, `Long turnId`, `int sequence`, `TurnEventType type`, `String content`, `String toolName`(nullable), `String toolCallId`(nullable), createdAt, updatedAt
- 팩토리(필드 사용규약 캡슐화): `userMessage/assistantMessage(turnId,seq,content)`, `toolCall(turnId,seq,toolName,toolCallId,content)`, `toolResult(turnId,seq,toolCallId,content)`
- `TurnEventIdentity`(eventId), `TurnEventType` enum: USER_MESSAGE/TOOL_CALL/TOOL_RESULT/ASSISTANT_MESSAGE
- 도메인 field `sequence` ↔ 엔티티/컬럼 `seq`

## schedule (com.chatbot.bravo.model.schedule)

### Schedule (@Value, AuditFields)
- 필드: `Long scheduleId`, `Long userId`, `Long turnId`(생성 출처 턴), `String title`, `String content`(nullable), `ScheduleType scheduleType`, `Instant scheduledAt`(UTC), `Instant doneAt`(nullable, null=미완료), createdAt, updatedAt
- 팩토리: `create(userId, turnId, title, content, scheduleType, scheduledAt)` — 미완료로 생성
- 상태변경: `done()` — doneAt 세팅한 새 인스턴스 / 판정: `isDone()`
- Read 모델 없음 — 단일 테이블 전량 반환, Schedule이 읽기/쓰기 겸용 (조인 프로젝션 필요 시 분리)

### ScheduleIdentity (@Value)
- `Long scheduleId`

### ScheduleType (enum)
- 값: HEALTH / PERSONAL / WORK / ETC
- `fromOrEtc(String)` — 대소문자 무관, 미스매치·null은 ETC 흡수 (LLM 툴 인자 안전망을 enum이 소유)
