# Plan: Schedule 도메인 추가 (API 레이어 없음)

## 요청 스펙

- **진입점**: 추후 챗 툴 핸들러(schedule_add / schedule_list, 이후 schedule_done). REST API 없음.
- **대상 레이어**: model / infrastructure / repository-jdbc 만. (usecase/api는 툴 프레임 착수 시 별도)

### 테이블: schedules

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGINT AUTO_INCREMENT | PK |
| user_id | BIGINT | NOT NULL |
| turn_id | BIGINT | NOT NULL — 생성 출처 턴 |
| title | VARCHAR(200) | NOT NULL |
| content | TEXT | NULL |
| schedule_type | VARCHAR(20) | NOT NULL — HEALTH/PERSONAL/WORK/ETC, enum 미스매치는 ETC 흡수 |
| scheduled_at | DATETIME(6) | NOT NULL, UTC |
| done_at | DATETIME(6) | NULL — NULL=미완료 |
| is_deleted / deleted_at / created_at / updated_at | 표준 감사 컬럼 | |

- 인덱스: `idx_schedules_user_scheduled(user_id, scheduled_at)`
- FK 미사용 (프로젝트 규약)

### 포트: ScheduleRepository

- `Schedule save(Schedule)` — 신규 생성 + 상태 변경(done, soft delete) 통일
- `List<Schedule> findAllByUserIdInPeriod(Long userId, Instant from, Instant to)` — scheduled_at ASC, from/to 필수(기본값 해석은 usecase 책임)
- `Optional<Schedule> findByIdAndUserId(Long scheduleId, Long userId)` — done 툴용 단건. userId 스코프 쿼리로 타 유저 접근 원천 차단
- 모든 쿼리에 `is_deleted = FALSE` 기본 포함

## Phase 1: 평가

### assess-api

스킵 — REST API 없음. 진입점은 추후 챗 툴 핸들러.

### assess-usecase

스킵 — usecase는 툴 프레임(schedule_add/list 핸들러) 착수 시 별도 계획으로.

### assess-infra

**충족 여부**: 미충족 — schedule 도메인의 Repository 자체가 없음 (기존: auth, chat 뿐). 신규 생성.

**model에 필요한 기능 (신규 요청)**:
- 대상: schedule (신규 도메인)
- `Schedule` 도메인 모델 (@Value, AuditFields 규약):
  - 필드: `Long scheduleId`, `Long userId`, `Long turnId`, `String title`, `String content`(nullable), `ScheduleType scheduleType`, `Instant scheduledAt`, `Instant doneAt`(nullable), `Instant createdAt`, `Instant updatedAt`
  - soft delete 필드는 모델에 없음 — Entity 레이어 전용 (프로젝트 규약)
  - 팩토리: `create(userId, turnId, title, content, scheduleType, scheduledAt)` — 신규(id=null, doneAt=null)
  - 상태변경: `done()` — doneAt 세팅한 새 인스턴스 (Turn.complete() 패턴)
- `ScheduleType` enum: HEALTH / PERSONAL / WORK / ETC
  - `fromOrEtc(String)` 정적 팩토리 — 미스매치 값을 ETC로 흡수 (LLM 인자 안전망, 규약을 enum이 소유)
- `ScheduleIdentity` VO: `Long scheduleId` (규약)
- 왜: ScheduleRepository의 입출력 타입. 읽기/쓰기 모두 Schedule 단일 타입(단일 테이블 전량 조회라 별도 Read 프로젝션 불필요 — TurnRepository와 동일 패턴)

**현재 레이어 작업 계획**:
```
- [신규] ScheduleRepository (com.chatbot.bravo.infrastructure.schedule.repository)
  - Schedule save(Schedule schedule)
    — 신규 생성(id=null) + 상태변경(done, soft delete) 공용. 반환: Schedule
  - List<Schedule> findAllByUserIdInPeriod(Long userId, Instant from, Instant to)
    — scheduled_at ASC. from/to 필수(기본값 해석은 상위 책임). 반환: List<Schedule>
  - Optional<Schedule> findByIdAndUserId(Long scheduleId, Long userId)
    — userId 스코프 단건. 반환: Optional<Schedule>
  - 원자적 연산: 없음
  - 모든 조회에 is_deleted=false 필터 (JDBC 구현 책임)
```

### assess-model

**충족 여부**: 미충족 — schedule 도메인 자체가 없음 (기존: auth, chat 뿐). 신규 생성.

**작업 계획** (model이 최하위 — 하위 레이어 없음):
```
- [신규] Schedule (@Value, 순수 Java)
  - 필드: Long scheduleId, Long userId, Long turnId, String title,
          String content(nullable), ScheduleType scheduleType,
          Instant scheduledAt, Instant doneAt(nullable),
          Instant createdAt, Instant updatedAt
  - 팩토리: create(userId, turnId, title, content, scheduleType, scheduledAt)
          — scheduleId=null, doneAt=null로 생성
  - 상태변경: done() — doneAt 세팅한 새 인스턴스 반환 (Turn.complete() 패턴)
  - soft delete 필드 없음 (Entity 전용, 규약)

- Read 모델: 없음 — 단일 테이블 전량 반환이라 Schedule이 읽기/쓰기 겸용
  (TurnRepository 패턴. RecentTurn 같은 프로젝션은 조인 생길 때만)

- [신규] ScheduleIdentity (@Value)
  - 필드: Long scheduleId

- [신규] ScheduleType (enum)
  - 값: HEALTH, PERSONAL, WORK, ETC
  - 정적 팩토리: fromOrEtc(String) — 대소문자 무관 매칭, 미스매치·null은 ETC
    (LLM 툴 인자 안전망. 흡수 규약을 enum이 소유)
```

## Phase 2: 구현 계획

**평가 결과**:
- api: 스킵 (REST API 없음 — 진입점은 추후 챗 툴 핸들러)
- usecase: 스킵 (툴 프레임 착수 시 별도 계획)
- infra: 미충족 — ScheduleRepository 신규
- model: 미충족 — Schedule/ScheduleType/ScheduleIdentity 신규

**구현 순서 (역순, bottom-up)**:
1. add-model: Schedule + ScheduleType + ScheduleIdentity
2. add-infrastructure: ScheduleRepository 포트 (save / findAllByUserIdInPeriod / findByIdAndUserId)
3. add-entity-specs: ScheduleEntity + schedules DDL (schema.sql append, idx_schedules_user_scheduled)
4. add-jdbc-query: ScheduleEntityRepository + ScheduleJdbcRepository + 통합 테스트

usecase/api 단계 없음 — 툴 프레임(schedule_add/list 핸들러) 계획에서 이어받는다.

## Phase 3: 구현 진행

### add-model ✅
- 생성: `model/schedule/Schedule.java`, `ScheduleType.java`, `ScheduleIdentity.java`
- Turn 패턴 미러링(@Value + AuditFields, 팩토리/상태변경 새 인스턴스). isDone() 판정 메서드 포함.
- specs/models.md 갱신. 컴파일 통과.

### add-infrastructure ✅
- 생성: `infrastructure/schedule/repository/ScheduleRepository.java` (포트 3메서드)
- 기간 조회는 [from, to) 반개구간으로 계약 명시. specs/infrastructure.md 갱신. 컴파일 통과.

### add-entity-specs ✅
- 생성: `jdbc/schedule/repository/ScheduleEntity.java` (TurnEntity 미러링 — AuditFields 직접 구현, toDomain/from/softDelete)
- DDL: local_h2/schema.sql 에 `-- === Schedule ===` 블록 append (+ idx_schedules_user_scheduled)
- 이 프로젝트는 BaseEntity/MySQL DDL 미사용 — H2 한 벌 관리. 컴파일 통과.

### add-jdbc-query ✅
- 생성: `ScheduleEntityRepository`(derived query 2개), `ScheduleJdbcRepository`(포트 구현 — 변환·위임만), `ScheduleJdbcRepositoryTest`(7케이스)
- 기간 조회는 derived query로 [from, to) + scheduled_at ASC, id ASC 타이브레이커
- 테스트 검증: 저장/스코프 조회, 전 필드 온전성, done() update, [from,to) 경계, 유저 격리, 타 유저 차단, soft-delete 제외
- repository-jdbc + model 전체 테스트 통과

**Phase 3 완료 — schedule 도메인 model/infra/jdbc 층 구축 끝. 다음: 툴 프레임(schedule_add/list 핸들러 + usecase) 별도 계획.**
