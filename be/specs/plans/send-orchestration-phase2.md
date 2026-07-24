# Plan: SendMessage Phase 2 — 프롬프트 기반 툴 루프

> MVP(도구 없음) → 툴 루프. 유저 결정 2건 반영:
> (1) **seq 제거** — turn_events.id(auto-increment)로 순서 보장.
> (2) **툴 정보를 system prompt에 나열** → 네이티브 function-calling이 아닌 **프롬프트 기반 툴 콜**
>     → 이식한 `OpenAiClient`(구조화 출력) 재사용, Spring AI 네이티브 툴콜링 리스크 제거.
> 가정: 툴은 각각 별도 함수(핸들러)로 실행 / 이전 컨텍스트를 모든 툴에 동일하게 인자로 전달.

## 문제 1 — seq 제거, id로 순서 보장
- `turn_events.id` = BIGINT AUTO_INCREMENT. 한 턴 이벤트는 루프가 순차 append(동시성 없음) → 삽입 순서 = id 오름차순.
- 조회는 `ORDER BY id`. `appendAll`(배치)도 리스트 순서대로 id 부여됨.
- **변경**: schema에서 `seq` 컬럼 + `uk_turn_events_turn_seq` + `idx_turn_events_turn_type`(seq 무관은 유지) 정리 → `seq` 제거. `TurnEventEntity.seq`, `TurnEvent.sequence`, 팩토리 seq 인자, `findByTurnIdAndIsDeletedFalseOrderBySeqAsc`→`...OrderByIdAsc` 제거/수정.
- chat 스펙의 `sequence` 규약은 폐기(유저 결정). → 문제 1(seq 카운터/시퀀서) 통째로 소멸. `TurnRecorder`도 seq 책임 없이 얇아짐(이벤트 append + 턴 상태만; 선택).

## 문제 2 — 프롬프트 기반 툴 루프

### 시스템 프롬프트에 툴 목록
- 기존 `ToolGuidanceContributor`(지침) + **툴 목록 렌더링**(name/description/parameters)을 시스템 프롬프트 섹션에 포함.
- `ToolCatalog`가 `enabledTools`로부터 목록 텍스트/스키마 생성 → 시스템 프롬프트 조립에 주입.
- 모델 지시: "최종 답 또는 툴 호출 하나를 아래 JSON으로만 응답하라."
- **주의: enabledTools가 유저별 주입(P2)이라 시스템 프롬프트는 요청 시 조립.** `ChatSystemPromptProvider`(현 싱글턴) → `ChatSystemPromptFactory.build(enabledTools)`로 전환, 같은 enabledTools 조합은 캐시.

### 구조화 응답 (OpenAiClient 재사용)
- `LlmAction` (파싱 대상):
```json
{ "type": "FINAL" | "TOOL_CALL",
  "content": "최종 답 (FINAL일 때)",
  "tool": { "name": "get_time", "arguments": { ... } } }
```
- `OpenAiClient.callSingleType(GptRequest.of(model, systemWithTools, contextPrompt, LlmAction.class))`
  - CleanJson(코드블록 제거) + tolerant ObjectMapper 이미 내장 → JSON 흔들려도 복구.
- LLM 포트: 기존 `LlmClient`(메시지리스트/네이티브)는 **미사용**으로 두거나, `LlmActionClient` 개념으로 대체.
  - MVP의 `OpenAiLlmClient`(단발 텍스트)는 유지 가능하나, 툴 루프는 `OpenAiClient`(구조화) 경로 사용.

### 컨텍스트 전달 ("동일하게 인자로")
- 대화 컨텍스트를 **문자열로 렌더링**해 userPrompt로 전달. 매 반복 tool_call/result를 이어붙여 누적, 동일 방식으로 다음 호출에 전달.
- render 규약: history(USER/ASSISTANT) + 현재 입력 + (반복 시) `[tool_call: name(args)]` / `[tool_result: ...]` 블록. 턴 컨텍스트(<system-context> 날짜)도 여기.

### 툴 인프라 (service.chat.orchestrator.tool) — **인터페이스만, 구현 비움**
- `ToolContext(Long userId, List<LlmMessage> conversation)` — 모든 툴에 동일 전달(가정)
- `ToolHandler`: 툴 1개 = 구현 1개
  - `String name()` / `String description()` — 라우팅 키 + 시스템 프롬프트 목록 노출
  - `ToolOutcome handle(ToolInvocation call, ToolContext ctx)`
- **`ToolOutcome(String contextToLeave, boolean isError)`** — ★ **툴이 "대화에 남길 컨텍스트"를 스스로 정의**(요약/일부/전체). 규약을 각 툴이 소유.
- `ToolCatalog`: `name → ToolHandler`. 시스템 프롬프트용 목록 생성 + 실행 라우팅.
- `ToolExecutor`: name으로 핸들러 찾아 실행, 예외는 `isError`로 래핑.
- **루프의 표현**: 툴 결과를 messages에 append할 때 마커로 감싸 논의 대상만 표현 —
  ```
  ## tool start
  {name}({args})
  {ToolOutcome.contextToLeave}
  ## tool end
  ```

### 컨텍스트 = List<LlmMessage> (L1-B 확정)
- 메시지 리스트 유지. 툴 call/result도 append(모델 판단 근거가 messages에 있어야 함).
- 프롬프트 기반이라 툴 교환도 **텍스트 메시지(role+content)** — `LlmMessage`는 `(role, content)` 유지(+TOOL role 정도). toolCalls/toolCallId 필드 확장 불필요.
- `OpenAiLlmClient`가 `ChatModel`에 메시지 리스트 전달 → 응답 텍스트를 `LlmAction`으로 파싱(CleanJson+tolerant 재사용). `OpenAiClient.callSingleType`(단일 문자열)은 미사용.

### 루프 (DefaultSendMessageUsecase)
```
userId  = authenticateUser(sessionKey)
context = contextRenderer.render(history(userId,20) + userMessage + turnInjection)
turn    = openTurnWithUserMessage(userId, message)   # id로 순서, seq 없음
systemP = systemPromptProvider.withTools(enabledTools)

steps = 0
while true:
    if ++steps > MAX_STEPS: throw failTurn(turn, "overrun")          # ③ 가드
    action = openAiClient.callSingleType(systemP, context, LlmAction.class)

    if action.isFinal():                                             # ① 정상
        recordAssistant(turn, action.content()); return completeTurn(turn, action.content())

    recordToolCall(turn, action.tool())                             # TOOL_CALL 이벤트
    result = toolExecutor.execute(action.tool(), new ToolContext(userId, context))  # 별도 함수
    recordToolResult(turn, result)                                  # TOOL_RESULT 이벤트
    context = contextRenderer.appendToolExchange(context, action.tool(), result)    # 누적
    # 치명 실패 시 예외 → failTurn → ② 종료
```

## 컴포넌트 맵
| 컴포넌트 | 위치 | 상태 |
|---|---|---|
| `LlmAction`(+Tool DTO) | model.llm | 신규 (구조화 응답) |
| `ToolContext`/`ToolHandler`/`ToolCatalog`/`ToolExecutor` | service.chat.orchestrator.tool | 신규 |
| 컨텍스트 renderer | service.chat.orchestrator | 신규 (문자열 렌더/누적) |
| 시스템프롬프트 + 툴목록 | orchestrator.systemprompt | ToolGuidance 확장 or 신규 섹션 |
| `OpenAiClient` (구조화) | llm-openai | **재사용** (이미 이식됨) |
| TurnEvent seq | model/repo-jdbc/schema | **제거** (id 정렬) |
| `DefaultSendMessageUsecase` | service.chat.impl | 루프로 교체 |

## 결정 필요
| # | 항목 | 제안 |
|---|---|---|
| P1 | MAX_STEPS | **8** (동기 루프의 지역 int 카운터 — DB/토큰 집계 없음, 무비용. 곧 LLM 호출 상한=비용 상한). 선택: 월-클럭 타임아웃 추가. ※재개형 비동기 전환 시엔 turn_events의 TOOL_CALL 개수로 재개 시 1회 집계 |
| P2 | enabledTools 출처 | **외부 주입(유저 컨텍스트 기반)** — `EnabledToolsResolver(userId)` 인터페이스만 잡고 구현은 별도. **앱 고정 아님** → 시스템 프롬프트(툴목록)는 싱글턴 불가, enabledTools 인자로 요청 시 조립(같은 조합 캐시 가능) |
| P3 | 툴 에러 | **보류** |
| P4 | 모델 | OpenAiClient의 GptModel(예: FOUR_MINI). 툴 루프엔 지시추종 좋은 모델 |
| P5 | seq 컬럼 | **제거 확정** (id 정렬, 스펙 sequence 규약 폐기) |
| P6 | 첫 툴 | **보류** |
| P7 | 컨텍스트 렌더 형식 | 텍스트 블록(위 render 규약). 나중에 토큰예산 절단 |

## 단계
1. seq 제거 (schema/entity/model/repo/usecase) — MVP 동작 보존
2. `LlmAction` + 툴 DTO (model.llm)
3. 시스템 프롬프트에 툴 목록 섹션 + `ToolCatalog`
4. `ToolHandler`/`ToolExecutor` + 더미 툴(get_time)
5. 컨텍스트 renderer + `OpenAiClient` 구조화 호출 경로
6. `DefaultSendMessageUsecase` 루프 교체
7. 테스트(툴1회/반복/가드/에러 파싱) + 실기동 E2E(더미 툴 실호출)
