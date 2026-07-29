# Usecases (현재 상태)

> add-usecase가 갱신. assess-usecase/assess-api가 참조. usecase는 @Service 구체 클래스.

## auth (com.chatbot.bravo.service.auth)

### LoginUsecase (@Service)
- `LoginResult login(LoginCommand)` — findByUsername → PasswordVerifier.matches → SessionManager.newOne
- 실패: 계정없음/비번불일치 모두 `LoginFailedException(401)` (구분 안 함)

### LogoutUsecase (@Service)
- `void logout(LogoutCommand)` — SessionManager.revoke(sessionKey). 소유권 검사 없음(인증 계층 도입 전), 멱등

### 지원 컴포넌트
- `PasswordVerifier`(interface) + `BcryptPasswordVerifier`(@Component, spring-security-crypto)
- `SessionManager`(interface: newOne/check/revoke) + `DefaultSessionManager`(@Service, @Transactional)
  - `check(sessionKey)`: 세션 조회+만료판정(InvalidSessionException 401)+touch — 인증 계층 도입 시 사용(현재 미사용)

### DTO (record)
- Command: `LoginCommand(username, rawPassword)`, `LogoutCommand(sessionKey)`
- Result: `LoginResult(sessionKey)`

## chat (com.chatbot.bravo.service.chat)

### GetRecentTurnsUsecase (@Service, read)
- `GetRecentTurnsResult getRecentTurns(GetRecentTurnsQuery)` — size 검증(1~20, 초과 시 `InvalidRecentTurnSizeException` 400) → RecentTurnQueryRepository 조회
- DTO(record): `GetRecentTurnsQuery(Long userId, int size)`, `GetRecentTurnsResult(List<RecentTurn> turns)`

### SendMessageUsecase (인터페이스 — 구현 이연)
- `SendMessageResult sendMessage(SendMessageCommand)`
- DTO(record): `SendMessageCommand(Long userId, String message)`, `SendMessageResult(Long turnId, String message, Instant createdAt)`
- **현재 스텁** `NotImplementedSendMessageUsecase`(@Service) → `SendMessageNotImplementedException`(501)
- **이연**: Turn 생성 → USER_MESSAGE append → LLM 호출 → 툴 루프(TOOL_CALL/RESULT) → ASSISTANT_MESSAGE append → Turn 완료
- `LlmClient` (빈 마커 인터페이스): OpenAI 자리, 시그니처는 툴 루프 설계 후 (decision 5)

## schedule (com.chatbot.bravo.service.schedule)

### ScheduleReader (@Service, read)
- `List<Schedule> readInPeriod(Long userId, LocalDate from, LocalDate to, int size)`
  — from null=오늘(KST), to null=from+6일(총 7일), to는 포함(inclusive),
  scheduled_at 역순(최신순), size 1~100 클램프. turn(대화)과 무관 — API·툴 공용
- 소유권: 쿼리 레벨(userId 조건) 강제 — Reader에서 별도 검증 없음

### ScheduleWriter (@Service, write — 모든 public 메서드 @Transactional)
- `Schedule create(Long userId, Long turnId, String title, String content, ScheduleType, Instant scheduledAt)`
  — turnId null = API 발 생성
- `Schedule replace(Schedule old, Long turnId, String newTitle, String newContent, ScheduleType newType, Instant newAt)`
  — **변경(교체) 정책의 유일 구현**: 새 row 저장 → 기존 row softDelete. null 파라미터는 기존 값 유지.
  반환 Schedule의 id가 바뀜. 챗 툴(applyUpdate)이 직접 사용
- `Schedule replaceById(Long userId, Long scheduleId, ...변경값)` — findByIdAndUserId → 없으면
  `ScheduleNotFoundException`(404, 존재 은닉) → replace(turnId=null) 위임. API 경로
- `void delete(Long userId, Long scheduleId)` — softDelete 0행이면 `ScheduleNotFoundException`

### 컨슈머
- ScheduleToolHandler(챗 툴): add→Writer.create, apply_update→Writer.replace.
  조회(list·update 후보 검색)는 ScheduleRepository 직접 사용 (제목 매칭·캡 등 대화 프레젠테이션 정책은 툴 소유)
- ScheduleApiController(REST): Reader.readInPeriod / Writer.create·replaceById·delete
