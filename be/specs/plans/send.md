# Plan: SendMessage (POST /chat/turns) — 데이터 레이어 + 오케스트레이션 스텁

> be-planner 배치 3. 유저 결정: **데이터 레이어 전부 구축, SendMessageUsecase·LlmClient는 빈 인터페이스, 엔드포인트 501 스텁.**
> 오케스트레이션(LLM 툴 루프)은 어려운 부분이라 이연 — 나중에 천천히 설계. OpenAI 미구현.
> `turns`/`turn_events` 테이블은 배치 2에서 생성됨 → **DDL 변경 없음**, 매핑만.

## 만드는 것 (concrete)
### model (com.chatbot.bravo.model.chat)
- `Turn`(@Value, AuditFields): turnId, userId, TurnStatus status, Instant completedAt(nullable), String failureReason(nullable), createdAt, updatedAt
  - `start(userId)` → PROCESSING / `complete()` → COMPLETED+completedAt / `fail(reason)` → FAILED+completedAt+failureReason
- `TurnIdentity`(turnId)
- `TurnStatus` enum: PROCESSING, COMPLETED, FAILED
- `TurnEvent`(@Value, AuditFields): eventId, turnId, int sequence, TurnEventType type, content, toolName(nullable), toolCallId(nullable), createdAt, updatedAt
  - 팩토리(필드 사용규약 캡슐화): `userMessage(turnId,seq,content)`, `assistantMessage(turnId,seq,content)`, `toolCall(turnId,seq,toolName,toolCallId,content)`, `toolResult(turnId,seq,toolCallId,content)`
- `TurnEventIdentity`(eventId)
- `TurnEventType` enum: USER_MESSAGE, TOOL_CALL, TOOL_RESULT, ASSISTANT_MESSAGE

### infrastructure (com.chatbot.bravo.infrastructure.chat.repository)
- `TurnRepository`: `Turn save(Turn)`, `Optional<Turn> findById(Long)`
- `TurnEventRepository`: `TurnEvent append(TurnEvent)`, `List<TurnEvent> appendAll(List<TurnEvent>)`, `List<TurnEvent> findAllByTurnIdOrderBySequence(Long turnId)`

### repository-jdbc (com.chatbot.bravo.jdbc.chat.repository)
- `TurnEntity`(@Table("turns"), enum→VARCHAR 매핑) + Entity/Jdbc Repo
- `TurnEventEntity`(@Table("turn_events"), 필드 `seq`↔도메인 `sequence`) + Entity/Jdbc Repo
  - EntityRepo: `findByTurnIdAndIsDeletedFalseOrderBySeqAsc`

### service (com.chatbot.bravo.service.chat)
- **`SendMessageUsecase` (인터페이스만)**: `SendMessageResult sendMessage(SendMessageCommand)`
- **`LlmClient` (빈 인터페이스, 마커)**: OpenAI 연동 자리. 시그니처는 툴 루프 설계 확정 후 (decision 5, 별도 관리)
- **stub impl** `NotImplementedSendMessageUsecase`(@Service) → `SendMessageNotImplementedException`(501) 던짐 (의존성 주입 없음)
- DTO(record): `SendMessageCommand(Long userId, String message)`, `SendMessageResult(Long turnId, String message, Instant createdAt)`

### exception (com.chatbot.bravo.exception.chat)
- `SendMessageNotImplementedException`(501, "아직 구현되지 않은 기능입니다")

### api (com.chatbot.bravo.api.chat)
- `ChatApiController.sendMessage`: `POST /chat/turns?userId=`(interim) + body `{message}` → 현재는 501
- Request `SendMessageRequest`(message @NotBlank @Size(max=4000)) / Response `SendMessageResponse`(turnId, message, createdAt)

## 비우는 것 (이연)
- `SendMessageUsecase` 구현체의 실제 로직: Turn 생성 → USER_MESSAGE append → LLM 호출 → 툴 루프(TOOL_CALL/RESULT append) → ASSISTANT_MESSAGE append → Turn 완료 → 응답
- `LlmClient` 실제 구현 (OpenAI)

## 결정 지점
| # | 항목 | 제안 |
|---|---|---|
| S1 | userId(인증 이연) | 임시 쿼리파라미터 `?userId=` (recent와 동일) |
| S2 | message 검증 | @NotBlank + @Size(max=4000) → 위반 시 400 |
| S3 | seq 매핑 | 도메인 field `sequence` ↔ 엔티티/컬럼 `seq` |
| S4 | enum 저장 | Spring Data JDBC enum→VARCHAR 자동 매핑 (status, type) |
| S5 | 미구현 응답 | `SendMessageNotImplementedException` → **501** |

## 검증
- compile green
- bootRun: 앱 기동(쓰기 Repo 빈 로드 확인) + `POST /chat/turns` → 501 + 검증(빈 message) → 400 + 회귀(Auth/recent 유지)
- 쓰기 Repo는 이번엔 소비처(스텁)가 안 씀 → 빈 등록만 확인. 실제 저장 동작은 오케스트레이션 구현 시 검증.

---

## Phase 3: 구현 진행 (완료 2026-07-23)

데이터 레이어 전부 구현 + 오케스트레이션/LLM 스텁. compile green, bootRun 검증 통과.

**검증:**
| 시나리오 | 결과 |
|---|---|
| POST /chat/turns(정상 요청) → 501 + `SendMessageNotImplementedException` | ✅ |
| 빈 message → 400 / userId 누락 → 400 / message>4000 → 400 | ✅ |
| 쓰기 Repo(TurnJdbc/TurnEventJdbc) 빈 정상 로드 | ✅ |
| 회귀: recent 3 turns, login 204 | ✅ |
| swagger: /auth/login, /auth/session, /chat/turns, /chat/turns/recent 노출 | ✅ |

**상태 문서 갱신:** specs/api.md, usecase.md, infrastructure.md, models.md 에 Turn/TurnEvent + send 스텁 반영.

**남은 것 (다음 세션/작업):** SendMessageUsecase 오케스트레이션 구현(LLM 툴 루프) + LlmClient(OpenAI) 구현. 데이터 레이어는 준비 완료.
