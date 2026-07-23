# Plan: Recent 조회 (GET /chat/turns/recent)

> be-planner 배치 2. net-new (레퍼런스에 chat 도메인 없음) — Auth에서 확립한 컨벤션 적용.
> 핵심: **agg — 한 Turn에서 USER_MESSAGE + 최종 ASSISTANT_MESSAGE만, 중간 TOOL_CALL/TOOL_RESULT 제외.**

## 요청 스펙
- `GET /chat/turns/recent` — 로그인 유저의 최근 완료(COMPLETED) 대화 최대 20개
- 응답: `{ turns: [{ turnId, userMessage, assistantMessage, createdAt }] }`, 없으면 `{ turns: [] }`
- 정렬: 오래된→최신 (화면 출력용)
- 실패: 401(세션), 400(size 범위), 500

## 배치 경계
- **읽기 경로만** 구현. `turns`/`turn_events` 테이블 DDL은 이 배치가 소유(생성).
- Turn/TurnEvent **도메인 모델 · 엔티티 · 쓰기 Repository · enum(TurnStatus/TurnEventType) 은 배치 3(send)로 이연**. 여기선 raw SQL 리터럴('COMPLETED','USER_MESSAGE','ASSISTANT_MESSAGE')로 처리.

## 평가 (Case A, 신규 chat 도메인 읽기 슬라이스)
- api: 미충족 → `GET /chat/turns/recent` 신규
- usecase: 미충족 → `GetRecentTurnsUsecase` 신규
- infra: 미충족 → `RecentTurnQueryRepository` 신규 (전용 read-only query repo)
- model: 미충족 → `RecentTurn` read projection 신규

## 결정 지점 (게이트 A 확인)
| # | 항목 | 제안 |
|---|---|---|
| G1 | userId 수신(인증 이연, 결정4) | **임시 쿼리파라미터 `?userId=`** (인터셉터 도입 시 세션에서 주입) |
| G2 | size | 쿼리파라미터 `size` 기본 20, 검증 1~20, 벗어나면 `InvalidRecentTurnSizeException`(400) |
| G3 | content 컬럼 / sequence | `content CLOB`, 예약어 회피 위해 컬럼명 **`seq`**(도메인 field는 sequence, 배치3에서 @Column 매핑) |
| G4 | Read 타입 | 단일 `RecentTurn`(model) 사용 — RecentTurnData/RecentTurnDto 중복 제거. api Response만 별도 |
| G5 | 정렬 구현 | SQL은 created_at DESC LIMIT N → **앱에서 reverse(ASC)** |
| G6 | 최종 assistant 판정 | **도메인 §7 불변식**(완료 Turn당 ASSISTANT_MESSAGE 정확히 1개) → 서브쿼리 없이 `type='ASSISTANT_MESSAGE'` 필터만. USER_MESSAGE도 정확히 1개 → 단순 조인으로 Turn당 1행 |

---

## 구현 계획 (bottom-up)

### 1. add-model (model 모듈, com.chatbot.bravo.model.chat)
- `RecentTurn` (@Value): `Long turnId`, `String userMessage`, `String assistantMessage`, `Instant createdAt`
  - (Turn/TurnEvent 도메인 모델은 배치3)

### 2. add-infrastructure (infrastructure, .infrastructure.chat.repository)
- `RecentTurnQueryRepository` (포트): `List<RecentTurn> findRecentCompletedTurns(Long userId, int size)`

### 3. add-entity-specs (schema DDL — turns, turn_events 소유)
```sql
-- === Chat ===
CREATE TABLE IF NOT EXISTS turns (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT      NOT NULL,
    status         VARCHAR(20) NOT NULL,
    completed_at   DATETIME(6),
    failure_reason TEXT,
    is_deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at     DATETIME(6),
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_turns_user_status_created ON turns(user_id, status, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS turn_events (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    turn_id      BIGINT      NOT NULL,
    seq          INT         NOT NULL,
    type         VARCHAR(30) NOT NULL,
    content      CLOB        NOT NULL,
    tool_name    VARCHAR(100),
    tool_call_id VARCHAR(100),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at   DATETIME(6),
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    CONSTRAINT uk_turn_events_turn_seq UNIQUE (turn_id, seq)
);
CREATE INDEX IF NOT EXISTS idx_turn_events_turn_type ON turn_events(turn_id, type);
CREATE INDEX IF NOT EXISTS idx_turn_events_tool_call ON turn_events(tool_call_id);
```
- data.sql 시드: tester(userId=1)의 완료 Turn 2~3개 + 각 Turn에 USER_MESSAGE / (TOOL_CALL/TOOL_RESULT) / ASSISTANT_MESSAGE 이벤트 — 툴 이벤트가 결과에서 제외되는지 검증용

### 4. add-jdbc-query (repository-jdbc, .jdbc.chat.repository)
- `RecentTurnQueryJdbcRepository` implements RecentTurnQueryRepository
  - `NamedParameterJdbcTemplate` + RowMapper→RecentTurn
  - **핵심 SQL** (agg, 중간 이벤트 제외 — 상관 서브쿼리 없음, 단순 조인):
```sql
SELECT t.id AS turn_id,
       u.content AS user_message,
       a.content AS assistant_message,
       t.created_at AS created_at
FROM turns t
JOIN turn_events u
     ON u.turn_id = t.id AND u.type = 'USER_MESSAGE'      AND u.is_deleted = FALSE
JOIN turn_events a
     ON a.turn_id = t.id AND a.type = 'ASSISTANT_MESSAGE' AND a.is_deleted = FALSE
WHERE t.user_id = :userId AND t.status = 'COMPLETED' AND t.is_deleted = FALSE
ORDER BY t.created_at DESC, t.id DESC
LIMIT :size
```
  - 완료 Turn당 USER_MESSAGE·ASSISTANT_MESSAGE 각 1개(불변식) → 조인 결과 Turn당 1행. TOOL_* 는 type 필터로 자연 배제.
  - 인덱스: `turn_events(turn_id, type)` 가 두 조인을 커버.
  - 반환 후 앱에서 `Collections.reverse` (오래된→최신)

### 5. add-usecase (service, com.chatbot.bravo.service.chat)
- `GetRecentTurnsUsecase` (@Service, read): `getRecentTurns(GetRecentTurnsQuery) -> GetRecentTurnsResult`
  - size 검증(1~20) 실패 시 `InvalidRecentTurnSizeException`(400)
  - queryRepository.findRecentCompletedTurns(userId, size) → reverse → Result
- DTO(record): `GetRecentTurnsQuery(Long userId, int size)`, `GetRecentTurnsResult(List<RecentTurn> turns)`
- 예외: `InvalidRecentTurnSizeException`(400) — exception 모듈 .exception.chat

### 6. add-api (api, com.chatbot.bravo.api.chat)
- `ChatApiController.getRecentTurns`: `GET /chat/turns/recent?userId=&size=20` → 200 `RecentTurnsResponse`
  - userId 쿼리파라미터(interim), size 기본 20
- Response DTO: `RecentTurnsResponse{ List<RecentTurnResponse> turns }`, `RecentTurnResponse{ turnId, userMessage, assistantMessage, createdAt }` + `from(RecentTurn)`

### 7. 검증: compile + bootRun e2e (시드로 recent 조회, 툴 이벤트 제외·정렬·size 검증 확인)

---

## Phase 3: 구현 진행 (완료 2026-07-23)

읽기 슬라이스 전 레이어 구현 완료. compile green, bootRun e2e 통과.

**e2e (시드: tester=userId 1, Turn A/B/C 완료 + D PROCESSING, B는 툴 이벤트 포함):**
| 시나리오 | 결과 |
|---|---|
| recent(userId=1) → Turn A,B,C 오래된→최신 | ✅ |
| **B의 TOOL_CALL/TOOL_RESULT 제외**, user+최종 assistant만 | ✅ |
| D(PROCESSING) 제외 | ✅ |
| size=2 → 최근 2개(B,C) | ✅ |
| 없는 유저 → `{"turns":[]}` | ✅ |
| size=0 / size=21 → 400 | ✅ |
| userId 누락 / size 타입오류 → 400 | ✅ |
| createdAt UTC ISO-8601 (Z) | ✅ |

**추가 수정:** `WebRequestExceptionHandler`(@Order(0)) 신설 — Spring MVC 바인딩 실패를 400으로 매핑(기존엔 500). C-7 준수(별도 advice).

**상태 문서 갱신:** specs/api.md, usecase.md, infrastructure.md, models.md 에 chat/recent 반영.

**미작성:** be-test 자동 테스트. **이연:** Turn/TurnEvent 도메인 모델·엔티티·쓰기 Repo·enum → 배치 3.

## chat 스펙 대비 편차
- `RecentTurnData`(infra)/`RecentTurnDto`(usecase) → 단일 `RecentTurn`(model)로 통합 (G4)
- 시각: DTO `LocalDateTime` 대신 **Instant 통일**(F3), api 응답 직렬화는 ISO-8601 문자열
- `sequence` 컬럼 → `seq` (예약어 회피, G3)
