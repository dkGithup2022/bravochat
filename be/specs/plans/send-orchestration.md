# Plan: SendMessage 오케스트레이션 (대화 루프 + 컨텍스트 관리)

> `NotImplementedSendMessageUsecase` 스텁을 대체하는 실제 구현 설계.
> LLM = OpenAI(Spring AI tool calling). SystemPromptBuilder = 우리 서비스([[systemprompt]]).
> 청사진: additional_resources/system-prompt-complete.md §8.

## 목표 수도코드
```
messages = loadContext(sessionId)          # 최상단: sessionId→userId→최신 히스토리
messages += userMessage
messages += turnContextInjection           # 날짜/시간대 (매 턴)

turn = TurnRepository.save(Turn.start(userId))   # PROCESSING
appendEvent(USER_MESSAGE)

while true:
    assistant = llm.call(systemPrompt, tools, messages)   # 외부 호출
    if assistant.hasNoToolUse():
        appendEvent(ASSISTANT_MESSAGE)
        turn.complete(); return SendMessageResult
    appendEvents(TOOL_CALL...)                # LLM이 요청한 툴 호출 기록
    results = toolExecutor.run(assistant.toolUses)
    appendEvents(TOOL_RESULT...)
    messages += assistant
    messages += results
    # (가드: guard++ > MAX_STEPS → fail)
```
도구 0개면 첫 호출에서 바로 END_TURN → 순수 대화봇도 같은 코드로 처리(문서 §8).

---

## 1. 최상단 — sessionId → userId → 최신 컨텍스트 쿼리

- 컨트롤러가 `Authorization: Bearer {sessionKey}` 파싱 → `SendMessageCommand(sessionKey, message)`.
- 유즈케이스 최상단: `SessionManager.check(sessionKey)` → `LoginSession` → `userId`.
  - (인증 계층을 지금 이 흐름에 인라인으로 연결 — 세션 없음/만료 시 `InvalidSessionException(401)`. decision 4의 임시 userId 파라미터를 여기서 대체.)
- `userId`로 **최신 대화 히스토리 로드**: `RecentTurnQueryRepository.findRecentCompletedTurns(userId, HISTORY_SIZE)`.
  - 이미 USER+최종ASSISTANT만 agg(TOOL_* 제외, 오래된→최신) → LLM 메시지로 바로 변환 가능. 재사용.

## 2. 컨텍스트 관리 (핵심)

### 2.1 메시지 표현 (인메모리, persist와 분리)
- `LlmMessage`(role: USER/ASSISTANT/TOOL, content, toolCalls?, toolCallId?) — 루프가 다루는 대화 메시지.
- `ToolCall`(id, name, argumentsJson), `ToolResult`(toolCallId, content, isError).
- persist용 `TurnEvent`와 **별개** — 경계에서 매핑(§4).

### 2.2 컨텍스트 조립 순서
```
[히스토리]  RecentTurn N개 → 각 Turn을 [USER, ASSISTANT] 2메시지로 평탄화 (오래된→최신)
[현재 입력] LlmMessage.user(message)
[턴 주입]   TurnContextInjector.buildTurnContext(now) → <system-context> (메시지 끝)
```
시스템 프롬프트(SystemPromptBuilder)는 messages가 아니라 별도 `system` 파라미터.

### 2.3 히스토리 정책 (결정 필요 D2)
- 1차: **최근 N턴 고정**(제안 N=10). 2차: 토큰 예산 기반 슬라이딩.
- 히스토리엔 TOOL_* 제외(RecentTurn이 이미 그럼) — 내부 실행 기록은 재현 안 함.

### 2.4 턴 내부 누적
- 루프가 돌 때마다 `messages += assistant`, `messages += toolResults`. tool_result는 대응 toolCallId로 짝맞춤(안 맞으면 LLM 400).

## 3. 나오는 구현체 (컴포넌트 맵)

### 포트/인터페이스 (헥사고날 — repository 패턴과 동일 배치)
| 컴포넌트 | 위치 | 책임 |
|---|---|---|
| `LlmClient`(포트, 재정의) | **infrastructure**.llm | `LlmResponse call(systemPrompt, tools, messages)` — provider 중립 |
| `ToolExecutor` | service.chat.orchestrator.tool | `ToolResult execute(ToolCall)` |
| `ToolCatalog`/`ToolDefinitionProvider` | service.chat.orchestrator.tool | 활성 툴 정의 제공 + 실행 라우팅 |
| `TurnContextInjector` | service.chat.orchestrator | 매 턴 날짜/시간대 주입 |
| `SystemPromptBuilder` | service.chat.orchestrator.systemprompt | ✅ 완료 |

### 값 타입
- `LlmMessage`, `ToolCall`, `ToolResult`, `LlmResponse`(stopReason, text, toolCalls) → **model.llm** (순수).

### 오케스트레이터
- `DefaultSendMessageUsecase`(@Service, service.chat.impl) — 위 수도코드. `NotImplementedSendMessageUsecase` 대체.
  - 주입: SessionManager, RecentTurnQueryRepository, TurnRepository, TurnEventRepository, LlmClient, ToolExecutor, SystemPromptBuilder, TurnContextInjector.

### 어댑터 (llm-openai 모듈)
- `OpenAiLlmClient` implements `LlmClient` — **Spring AI tool calling**(ChatModel + ToolCallback). 기존 `OpenAiClient`(단발성 구조화 출력)와 별개 — 루프는 멀티턴+툴콜이라 새로 필요.
- 의존 방향: llm-openai → model.llm + infrastructure.llm(포트). api-application이 include.

## 4. persist 매핑 & 시퀀스
- `Turn.start` 저장(PROCESSING) → 이후 각 이벤트를 `TurnEventRepository.append`로 순서대로.
- seq: 턴 내 단조 증가(EventSequencer 또는 로컬 카운터). USER=1, 이후 TOOL_CALL/RESULT/ASSISTANT 순.
- 매핑: `LlmMessage`/`ToolCall`/`ToolResult` → `TurnEvent.userMessage/assistantMessage/toolCall/toolResult`(이미 있는 팩토리).
- 종료: 성공 `turn.complete()`, 예외 `turn.fail(reason)` 저장.

## 5. 트랜잭션 & 에러 (설계 주의)
- **루프 전체를 @Transactional로 감싸지 말 것** — LLM 호출은 느린 외부 I/O. DB 커넥션을 그동안 잡으면 안 됨.
  - 각 이벤트 append / turn 상태 저장은 **짧은 개별 트랜잭션**.
- 가드: `MAX_STEPS`(제안 10) 초과 → `turn.fail` + 예외.
- 툴 에러: `ToolResult.isError=true`로 모델에 되먹임(문서 §8) — 루프 계속. 치명적/LLM 실패 → `turn.fail` + `LlmExecutionException(500)`.
- 예외(exception.chat): `LlmExecutionException`, `ToolExecutionException`, `TurnCompletionFailedException` (usecase-dto 스펙 §4).

## 6. 단계적 구현 (권장 — 리스크 분리)
- **Phase 1 (도구 없음, 순수 대화 E2E)**: LlmClient(OpenAI) + 컨텍스트 로드(session→userId→history) + 루프(첫 호출 END_TURN) + Turn/이벤트 persist. → 실제 대화 저장·기억 동작 확인. ToolExecutor/Definition 없이.
- **Phase 2 (툴)**: ToolExecutor + ToolCatalog + tool-calling 왕복. TOOL_CALL/RESULT persist.
- Phase 1만으로도 "대화하고 히스토리 기억하는 챗봇"이 완성됨.

## 7. 결정 필요
| # | 항목 | 제안 |
|---|---|---|
| D1 | userId 출처 | **sessionKey → SessionManager.check → userId** (chat에 세션 인증 인라인 연결). 임시 userId 파라미터 제거 |
| D2 | 히스토리 정책 | 최근 **N=10턴** 고정(1차), 토큰예산(2차) |
| D3 | 포트 배치 | LlmClient/메시지타입을 infrastructure.llm/model.llm로 이동(현재 service.chat.LlmClient 스텁 대체). repository 패턴과 동일 |
| D4 | 착수 범위 | **Phase 1(도구 없음)부터** — 대화 E2E 먼저, 툴은 Phase 2 |
| D5 | 기존 OpenAiClient | 유지(단발성 구조화 출력은 다른 용도). 루프용 `OpenAiLlmClient` 신규 |
| D6 | SendMessageResult | turnId + 최종 assistant text + createdAt (스텁 시그니처 유지) |
