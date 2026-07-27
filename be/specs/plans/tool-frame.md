# Plan: 툴 프레임 + schedule 툴 (블랙박스/서브에이전트 패턴)

## 설계 결정 (확정)

- **툴 = 블랙박스.** 외부(메인 루프/카탈로그/익스큐터)는 name + description만 안다.
  내부 동작 선택(add/list)·인자 추출은 툴이 자체 LLM 콜로 해결한다.
- 외부 노출 툴은 `schedule` **하나**. add/list는 내부 오퍼레이션 (parametersSpec 개념 없음).
- 메인 모델의 TOOL_CALL은 이름만 — instruction 동봉은 판단 분열이 실제 관찰되면 추가.
- 툴 내부 LLM 콜 입력 = `ToolContext.conversation` (메모리 20턴 + 이번 입력 + 턴 컨텍스트
  + 같은 턴의 이전 툴 마커 블록 전부).
- 반환 계약은 `ToolResponse(response, turnMemo, success)` — 되먹임과 기록의 분리.
- 부작용 타이밍(쓰기 후 턴 실패 시 일정 잔존)은 수용 (기존 결정).

## 1단계: 계약 변경 — service/chat/agent/tool

```java
// [신규] ToolResponse — ToolOutcome 대체(삭제)
record ToolResponse(String response, String turnMemo, boolean success) {
    static ok(response)            // turnMemo = response
    static ok(response, turnMemo)
    static fail(reason)            // turnMemo = "FAILED: " + reason
}

// [변경] ToolHandler — parametersSpec 없음, 블랙박스 계약
interface ToolHandler {
    String name();
    String description();          // 메인 프롬프트에 노출되는 전부
    ToolResponse handle(ToolInvocation call, ToolContext ctx);
}

// [변경] ToolContext — turnId 추가 (schedules.turn_id NOT NULL), enabledTools 추가 (익스큐터 검증용)
record ToolContext(Long userId, Long turnId, Set<String> enabledTools, List<LlmMessage> conversation)

// [유지] ToolInvocation(name, arguments) — arguments는 대부분 빈 맵
```

**LlmClient는 변경하지 않는다.** 프레임은 LLM을 모른다. 툴 내부의 추출 호출은
해당 툴이 소유한 별도 포트(예: `ScheduleOpExtractor` — service에 인터페이스,
llm-openai에 구현)로 해결 — 3단계의 구현 디테일이며 프레임 계약과 무관.

**[변경] DefaultSendMessageUsecase.applyToolCall**:
- tool_call_id UUID 서버 발급 → TOOL_CALL/TOOL_RESULT 이벤트 짝 기록
- TOOL_RESULT 저장 ← `turnMemo` / messages 마커 append ← `response`
- success=false → 마커에 실패 표시 포함 (모델 자가수정 유도, 턴은 안 죽임)
- ToolContext에 turn.getTurnId(), enabledTools 전달

## 2단계: Default 구현 3종 (Empty/No 스텁 삭제)

| 신규 | 동작 |
|---|---|
| `DefaultToolCatalog` | DI `List<ToolHandler>` → name 맵. renderToolSection = enabled만 `- name: description` 렌더 |
| `DefaultToolExecutor` | 미등록/비활성 → `ToolResponse.fail` / 핸들러 예외 catch → fail. **예외를 밖으로 안 던짐** |
| `AllRegisteredEnabledToolsResolver` | 등록 툴 전부 활성 |

**프롬프트 조정**: DefaultResponseFormatContributor의 TOOL_CALL 예시를 이름 중심으로
(`{"type":"TOOL_CALL","tool":{"name":"<툴 이름>","arguments":{}}}`),
DefaultToolGuidanceContributor 문구가 블랙박스 툴 목록과 정합한지 확인.

## 3단계: ScheduleToolHandler — service/chat/agent/tool/schedule

- name=`schedule`, description="유저의 일정 등록·조회 요청을 처리한다"
- 내부 흐름:
  1. 별도 추출 호출 — 툴 소유 포트 `ScheduleOpExtractor.extract(conversation)` (구현은 llm-openai 모듈)
     - 내부 프롬프트: 오퍼레이션 카탈로그(add/list) + 인자 스펙 + 오늘 날짜/Asia/Seoul
     - 출력: `{"op":"add","args":{...}}` | `{"op":"list","args":{...}}` | `{"op":"missing","question":"..."}`
  2. op 디스패치:
     - add: title 검증(공백/200자) + `scheduled_at` "YYYY-MM-DDTHH:mm" KST→UTC + type `fromOrEtc`
       → `Schedule.create(userId, ctx.turnId(), ...)` → save
       → response: "등록됨: [id] 2026-07-28(화) 15:00 [PERSONAL] 회의"
     - list: 기간 기본 오늘 0시(KST)~+7일 → `findAllByUserIdInPeriod`
       → response: 한 줄 요약 × 최대 20건 + "외 n건" ([id] 노출 — 향후 done 지칭용)
       → turnMemo: "schedule.list: n건 조회 (기간)"
  3. missing/파싱 실패/검증 실패 → `ToolResponse.fail(사유 or question)`

## 4단계: 테스트

- 단위: DefaultToolCatalog(렌더/라우팅), DefaultToolExecutor(성공/비활성/미등록/핸들러 예외),
  ScheduleToolHandler(LlmClient 스텁 — add/list/missing/JSON불량/KST변환/캡)
- 시나리오(api-application): FakeLlmClient 스크립트 —
  메인 1회차 `TOOL_CALL(schedule)` → 내부 추출 콜 → 메인 2회차 `FINAL`.
  검증: schedules 행 생성(turn_id 포함), TOOL_CALL/TOOL_RESULT 이벤트 짝(tool_call_id),
  transcript에 turnMemo 기록, 최종 응답 반환.
  ※ 메인 콜과 내부 콜이 같은 LlmClient 빈 — 스텁은 call()/rawCall() 메서드로 자연 구분됨.
- 수동 스모크: 실제 OpenAI로 "내일 3시 회의 잡아줘" → 등록 → "이번 주 일정 뭐 있지?" → 조회

## 구현 순서 (커밋 단위)

1. 계약: ToolResponse 신규 + ToolHandler/ToolContext 변경 + LlmClient.rawCall + 루프 매핑/tool_call_id
2. Default 3종 + 스텁 삭제 + 프롬프트 문구 조정
3. ScheduleToolHandler (내부 추출 콜 + op 디스패치)
4. 시나리오 테스트 + 수동 스모크

## 리스크/노트

- 툴 1회 = LLM 콜 +1 (동기 지연 수용 — 기존 결정)
- MAX_STEPS=8은 메인 콜 기준 유지 (내부 추출 콜은 스텝에 미포함)
- 내부 추출 콜 실패는 fail 되먹임 — LlmExecutionException(턴 FAILED)은 메인 콜 실패에만 한정

## 진행 기록

### 1~2단계 (프레임) ✅
- ToolResponse/ToolHandler/ToolContext/ToolExecutor 계약 + Default 3종 + 루프 매핑(tool_call_id, turnMemo/response 분리) + response_format 문구
- 카탈로그/익스큐터 단위 테스트 (LLM 무관 — 가짜 핸들러)

### 3단계 (AbstractToolHandler + schedule 툴) ✅
- 설계 변경: 핸들러 공통 뼈대를 템플릿 메서드로 — AbstractToolHandler<P>
  (extractParams는 ToolParamExtractor 포트(infrastructure.llm, LlmClient 무변경),
   구현 OpenAiToolParamExtractor(llm-openai, temp 0, 미지필드 무시), doToolLogic만 툴별)
- ScheduleParams(op: add|list|missing 평면 레코드) + ScheduleToolHandler
  (PARAM_SPEC 소유, KST→UTC, 검증, 기본기간 오늘~+7일, 캡 20건+[id], missing→fail(question))
- 단위 테스트 9케이스 (추출기 모킹)

### 4단계 (시나리오) ✅
- ScheduleToolScenarioTest: HTTP→툴 루프→DB 관통 (LlmClient+ToolParamExtractor만 모킹)
  등록 성공(행+이벤트4종+call_id 짝+프롬프트에 툴 목록 렌더 검증) / 정보부족(되묻기, 무저장, FAILED 메모)
- 남은 것: 실제 OpenAI 수동 스모크

### 리팩토링: ToolManager 도입 (카탈로그/리졸버 정리) ✅
- [삭제] ToolCatalog/DefaultToolCatalog, EnabledToolsResolver/AllRegisteredEnabledToolsResolver
- [신규] ToolManager — 각 툴을 생성자에서 **명시적으로** 주입받아 등록 (자동 List 수집 안 씀).
  라우팅(find) + 프롬프트 노출 목록(renderToolSection) + toolNames(조건부 섹션 판단) 관리
- ToolHandler.description() → promptText() — 시스템 프롬프트 노출 텍스트를 툴이 소유
- ToolContext = (userId, turnId, conversation) 3필드로 축소 (enabledTools 삭제 — 등록=활성)
- ChatSystemPromptProvider.build() 무인자화, DefaultSendMessageUsecase에서 resolver 제거
- 새 툴 추가 절차: 핸들러 구현 → ToolManager 생성자에 등록 한 줄
