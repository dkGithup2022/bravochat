# Plan: Schedules REST API (GET/POST/PATCH/DELETE /schedules)

> 4개 엔드포인트가 한 도메인 슬라이스(usecase/infra/model 공유)라 한 문서로 묶어 평가한다.

## 요청 스펙

### 엔드포인트
- `GET /schedules?from=&to=&size=20` — 기간 조회. from/to 생략 시 오늘(KST)~+7일,
  to는 포함(inclusive), scheduled_at 역순(최신순) 정렬, size 최대 건수
- `POST /schedules` — 등록
- `PATCH /schedules/{scheduleId}` — 변경. 정책은 챗 툴과 동일:
  새 row 추가 + 기존 row soft delete (응답의 scheduleId가 바뀜)
- `DELETE /schedules/{scheduleId}` — soft delete

### 정책 결정사항 (유저 확정)
- 인증: 기존 `@LoginUser LoginSession` 리졸버 재사용, userId는 세션에서만
- 소유권: 쿼리 레벨 강제 (`findByIdAndUserId`, `softDelete(id, userId)` 컨벤션),
  타 유저 일정 접근은 404 (존재 은닉)
- `turn_id`: nullable로 마이그레이션 (null = API 발 생성), origin 컬럼 없음
- 조회는 turn과 무관 (ScheduleReader)
- usecase 추출: ScheduleReader(조회) / ScheduleWriter(등록·변경·삭제) —
  챗 툴(ScheduleToolHandler)과 API가 공유. 변경(교체) 정책 구현은 Writer 한 곳에만
- 기존 자산 활용: `findByIdAndUserId`(미사용), `softDelete`(존재),
  `EntityNotFoundException`(404 베이스)

## Phase 1: 평가

### assess-api

**충족 여부: 미충족** — specs/api.md에 schedule 엔드포인트 없음 (auth/chat만 존재). 4개 전부 신규.

**하위 레이어 요청:**

A. usecase — 신규 요청 (specs/usecase.md에 schedule 섹션 없음):
- `ScheduleReader.readInPeriod(Long userId, LocalDate from, LocalDate to, int size): List<Schedule>`
  — from null=오늘(KST), to null=from+6일(총 7일), to inclusive, scheduled_at 역순, size 1~100 클램프
- `ScheduleWriter.create(Long userId, Long turnId, String title, String content, ScheduleType, Instant scheduledAt): Schedule`
  — turnId null 허용 (API 발 생성)
- `ScheduleWriter.replaceById(Long userId, Long scheduleId, ...변경값): Schedule`
  — 교체 정책(새 row + soft delete), 소유 아니면 ScheduleNotFoundException(404)
- `ScheduleWriter.replace(Schedule old, Long turnId, ...변경값): Schedule`
  — 교체 정책의 원형. 챗 툴(applyUpdate)이 직접 사용 (제목+시각으로 old를 이미 특정한 경로)
- `ScheduleWriter.delete(Long userId, Long scheduleId): void`
  — softDelete 실패(0행) 시 ScheduleNotFoundException(404)

B. model — 기존 사용 (변경 불필요):
- Schedule 필드(scheduleId, title, content, scheduleType, scheduledAt, doneAt)로 Response 구성 충분
- 단, `Schedule.create`의 turnId null 허용을 규약으로 명시 (javadoc — 코드 변경은 없음)

**API 레이어 작업 계획:**

| 엔드포인트 | Request | Response | 인증 | usecase |
|---|---|---|---|---|
| [신규] GET /schedules | query: from?, to? (YYYY-MM-DD), size?=20 | SchedulesResponse{schedules:[{scheduleId,title,content,scheduleType,scheduledAt,done}]} | 필요 | ScheduleReader.readInPeriod |
| [신규] POST /schedules | {title(@NotBlank,≤200), content?, scheduleType?, scheduledAt(@NotNull, Instant)} | ScheduleResponse | 필요 | ScheduleWriter.create(turnId=null) |
| [신규] PATCH /schedules/{scheduleId} | {title?, content?, scheduleType?, scheduledAt?} — 온 필드만 변경 | ScheduleResponse (새 scheduleId!) | 필요 | ScheduleWriter.replaceById |
| [신규] DELETE /schedules/{scheduleId} | - | 204 No Content | 필요 | ScheduleWriter.delete |

- scheduledAt 입출력은 ISO-8601 Instant(UTC) — KST 변환은 FE 책임
- 404 정책: 타 유저/없는 일정 동일하게 `ScheduleNotFoundException` → GlobalExceptionHandler(HttpException 404)
- Controller는 위임만: Request→파라미터 변환, Response.from(Schedule) 팩토리

### assess-usecase

**충족 여부: 미충족** — specs/usecase.md에 schedule 섹션 없음. 로직은 전부
ScheduleToolHandler(툴)에 있어 usecase 계층 신규 생성 + 툴에서 저장 정책 추출이 필요.

**하위 레이어 요청:**

A. infrastructure — 기존 사용 (신규 불필요):
- `ScheduleRepository.save` / `findAllByUserIdInPeriod` / `findByIdAndUserId` 사용
- `softDelete(scheduleId, userId)` — **코드에 이미 존재하나 specs/infrastructure.md 미반영**
  (이번 세션에 챗 툴 apply_update용으로 추가됨) → assess-infra에서 스펙 문서 동기화 판단
- 역순+limit 전용 쿼리는 만들지 않음 — Reader가 ASC 조회 후 메모리 reverse + size 캡
  (단일 유저 일정 규모에서 충분, 스펙 "limit 없음 — 노출 제한은 호출자가"와 일치)

B. model — 기존 사용 (변경 불필요):
- Schedule/ScheduleType으로 충분. Command 별도 DTO 없이 파라미터 전달
  (기존 chat usecase의 Query/Command 컨벤션과 다르지만, Reader/Writer 패턴에서는
  파라미터가 4개 이하로 단순 — 과설계 방지)
- `Schedule.create(userId, turnId, ...)`의 turnId null 허용 규약 명시 필요 (javadoc)

**usecase 작업 계획:**

- [신규] `ScheduleReader` (@Service, service/schedule/)
  - `readInPeriod(Long userId, LocalDate from, LocalDate to, int size): List<Schedule>`
  - 정책: from null=오늘(KST) / to null=from+6일 / to inclusive → [from 00:00, to+1일 00:00) KST /
    scheduled_at 역순 / size 1~100 클램프
  - 의존: ScheduleRepository / 트랜잭션: 불필요 (단건 조회)
- [신규] `ScheduleWriter` (@Service, service/schedule/)
  - `create(userId, turnId(null 허용), title, content, scheduleType, scheduledAt): Schedule`
  - `replace(Schedule old, Long turnId, 변경값...): Schedule` — 교체 정책 원형: 새 row 저장 → 기존 soft delete.
    null 파라미터는 기존 값 유지. 챗 툴 applyUpdate가 사용
  - `replaceById(userId, scheduleId, 변경값...): Schedule` — findByIdAndUserId → 없으면
    ScheduleNotFoundException(404, 신규 exception) → replace 위임. API가 사용
  - `delete(userId, scheduleId): void` — softDelete 0행이면 ScheduleNotFoundException
  - 의존: ScheduleRepository / 트랜잭션: replace·replaceById에 @Transactional (save+softDelete 원자화)
- [변경] `ScheduleToolHandler` — add()는 Writer.create로, applyUpdate()의 저장 블록은
  Writer.replace로 위임. 조회(list/update 후보 검색)는 기존 repository 직접 사용 유지
  (대화 프레젠테이션 정책 — 제목 매칭·캡 — 은 툴 소유)
- [신규] `ScheduleNotFoundException` (exception/schedule/, EntityNotFoundException 상속, 404)

### assess-infra

**충족 여부: 충족 (코드 기준)** — usecase가 요청한 메서드 4개 전부 `ScheduleRepository`에 존재:
`save`, `findAllByUserIdInPeriod`, `findByIdAndUserId`, `softDelete`.
신규/변경 메서드 없음.

**단, 부수 작업 2건:**
1. **스펙 문서 동기화** — `softDelete(scheduleId, userId): boolean`이 코드에는 있으나
   specs/infrastructure.md에 미반영 (이번 세션에 챗 툴 apply_update용으로 추가).
   Phase 3에서 infrastructure.md에 추기.
2. **turn_id nullable은 이 레이어 사안 아님** — 도메인 모델 `Schedule.turnId`는 참조 타입(Long)이라
   코드 변경 불필요. 실제 변경은 DDL(schema.sql)과 entity 규약 → **assess-model(entity-specs)로 내림**.

**하위 레이어 요청 (model):**
- 대상: schedule (신규 아님)
- DDL 변경: `schedules.turn_id` NOT NULL 제거 (null = API 발 생성) — 유저 확정 사항
- 기존 로컬 H2 DB에 idempotent ALTER 필요 (CREATE IF NOT EXISTS는 기존 테이블 미변경)
- `Schedule.create` javadoc에 turnId null 허용 규약 명시 (시그니처 변경 없음)

### assess-model

**충족 여부: 충족 (구조 변경 없음)** — Schedule/ScheduleType/ScheduleIdentity 기존 타입으로 충분.

- 도메인 모델(Write): `Schedule` 필드·팩토리·상태변경 그대로. `turnId`는 참조 타입(Long)이라
  null 표현 가능 — **코드 변경 없이 javadoc 규약만 변경**:
  - as-is: "챗 툴(schedule_add)로만 생성되며 turnId로 생성 출처 턴을 추적한다"
  - to-be: "챗 툴 또는 일정 API로 생성 — turnId로 생성 출처 턴 추적 (null = API 발 생성)"
- Read 모델: 불필요 (단일 테이블 전량 반환 — models.md 기존 결정 유지)
- Identity/enum/VO: 변경 없음
- **DDL 변경 (entity-specs 영역)**: `schedules.turn_id` NOT NULL 제거
  + 기존 로컬 DB용 idempotent `ALTER TABLE schedules ALTER COLUMN turn_id SET NULL;`
  + 스키마 주석 갱신 ("챗 툴로만 생성" → "챗 툴 또는 API로 생성")

## Phase 2: 구현 계획

**평가 요약:**
- api: **미충족** — 4개 엔드포인트 신규
- usecase: **미충족** — ScheduleReader/ScheduleWriter 신규 + 툴 핸들러 위임 리팩터 + ScheduleNotFoundException 신규
- infrastructure: **충족** — 메서드 전부 존재 (스펙 문서 동기화만)
- model: **충족** — javadoc 규약 + DDL(turn_id nullable)만

**구현 순서 (bottom-up):**
1. **add-entity-specs**: schema.sql turn_id nullable + idempotent ALTER + 주석,
   Schedule javadoc 규약 변경, models.md 갱신
2. **add-infrastructure** (문서만): infrastructure.md에 softDelete 추기
3. **add-usecase**: exception/schedule/ScheduleNotFoundException,
   service/schedule/ScheduleReader·ScheduleWriter 신규,
   ScheduleToolHandler add/applyUpdate → Writer 위임 리팩터 (+ 핸들러 테스트 수정),
   Reader/Writer 단위 테스트, usecase.md 갱신
4. **add-api**: ScheduleApiController + DTO 4종 (SchedulesResponse/ScheduleResponse/
   CreateScheduleRequest/UpdateScheduleRequest), ScheduleApiScenarioTest
   (POST→GET 역순→PATCH 교체·새 id→DELETE→404 은닉), api.md 갱신

## Phase 3: 구현 진행

### 1. add-entity-specs ✅
- schema.sql: `schedules.turn_id` NOT NULL 제거 + idempotent ALTER + 주석 갱신
- Schedule javadoc: turnId null=API 발 생성 규약 명시 (코드 변경 없음)
- specs/models.md: turnId nullable 규약 반영

### 2. add-infrastructure ✅ (문서 동기화만 — 평가 "충족"이라 코드 작업 없음)
- specs/infrastructure.md: `softDelete(scheduleId, userId): boolean` 추기

### 3. add-usecase ✅
- 신규: `exception/schedule/ScheduleNotFoundException`(404, 존재 은닉),
  `service/schedule/ScheduleReader`(readInPeriod — KST 기간·inclusive to·역순·size 클램프),
  `service/schedule/ScheduleWriter`(create/replace/replaceById/delete — 교체 정책 유일 구현, @Transactional)
- 리팩터: `ScheduleToolHandler` add→Writer.create, applyUpdate→Writer.replace 위임
  (조회는 repository 유지 — 프레젠테이션 정책은 툴 소유)
- 테스트: ScheduleReaderTest(3), ScheduleWriterTest(6) 신규,
  ScheduleToolHandlerTest·ToolManagerTest Writer 계약으로 수정 — 전부 통과
- specs/usecase.md: schedule 섹션 추가

### 4. add-api ✅
- 신규: `api/schedule/ScheduleApiController`(GET/POST/PATCH/DELETE /schedules, @LoginUser),
  DTO 4종(ScheduleResponse/SchedulesResponse record + CreateScheduleRequest/UpdateScheduleRequest)
- 테스트: `ScheduleApiScenarioTest` 3케이스 — CRUD 관통(역순·교체 새 id·soft delete·turn_id null),
  타 유저 404 존재 은닉, validation 400·미인증 401 — 전부 통과, 전체 빌드 통과
- specs/api.md: schedule 섹션 추가
- specs/fe-handoff-api.md: 2-5~2-8 엔드포인트 + 에러표 404 + S6 시나리오 추가 (FE 핸드오프)
