# API 엔드포인트 (현재 상태)

> add-api가 갱신. assess-api가 참조.

## auth (com.chatbot.bravo.api.auth, AuthApiController)

### POST /auth/login  (인증 불필요)
- Request: `LoginRequest{ username, password }` (@NotBlank)
- Success: `204 No Content` + 헤더 `Authorization: Bearer {sessionKey}`
- 실패: 400(검증), 401(LoginFailedException)
- usecase: `LoginUsecase.login`

### DELETE /auth/session  (로그아웃)
- Header: `Authorization: Bearer {sessionKey}` (required=false, 없거나 형식오류 시 InvalidSessionException 401)
- Success: `204 No Content`
- usecase: `LogoutUsecase.logout`

> 인증 cross-cutting(interceptor/argument-resolver)은 미구현 — 컨트롤러가 Authorization 헤더를 직접 파싱. 차후 인증 계층에서 대체 예정.

## chat (com.chatbot.bravo.api.chat, ChatApiController)

### GET /chat/turns/recent
- Query: `userId`(필수, **인증 이연으로 임시 파라미터**), `size`(기본 20)
- Success: `200 OK` `{ turns: [{ turnId, userMessage, assistantMessage, createdAt }] }` (오래된→최신, 완료 Turn만, TOOL_* 제외)
- 실패: 400(size 범위/파라미터), 없으면 `{ turns: [] }`
- usecase: `GetRecentTurnsUsecase.getRecentTurns`

### POST /chat/turns
- Query: `userId`(필수, 임시 파라미터), Body: `SendMessageRequest{ message }`(@NotBlank @Size(max=4000))
- Success(설계): `200 OK` `SendMessageResponse{ turnId, message, createdAt }`
- **현재: `501`** (SendMessageNotImplementedException — 오케스트레이션 미구현)
- 실패: 400(검증/파라미터)
- usecase: `SendMessageUsecase.sendMessage` (스텁)

## 공통 예외 핸들링
- `GlobalExceptionHandler`(be-init 소유): DomainException→상태코드 매핑, Bean Validation→400, 그 외→500
- `WebRequestExceptionHandler`(@Order(0), api-application): Spring MVC 바인딩 실패(필수 파라미터 누락/타입불일치/잘못된 바디)→400

## schedule (com.chatbot.bravo.api.schedule, ScheduleApiController)

### GET /schedules
- 인증 필요. Query: `from?`/`to?`(YYYY-MM-DD, to 포함, 생략 시 오늘 KST~+7일), `size?`(default 20, 1~100 클램프)
- 응답 200: `SchedulesResponse{schedules:[{scheduleId,title,content,scheduleType,scheduledAt(UTC),done}]}` — scheduled_at 역순(최신순)
- usecase: `ScheduleReader.readInPeriod`

### POST /schedules
- 인증 필요. Request: `CreateScheduleRequest{title(@NotBlank,≤200), content?, scheduleType?(미스매치 ETC 흡수), scheduledAt(@NotNull, Instant)}`
- 응답 201: `ScheduleResponse` — turn_id null(API 발 생성)
- usecase: `ScheduleWriter.create`

### PATCH /schedules/{scheduleId}
- 인증 필요. Request: `UpdateScheduleRequest` — 전 필드 optional, 온 필드만 변경
- 교체 방식(새 row + 기존 soft delete) — **응답의 scheduleId가 바뀜**
- 응답 200: `ScheduleResponse` / 404: 없는·타 유저 일정 (`ScheduleNotFoundException`, 존재 은닉)
- usecase: `ScheduleWriter.replaceById`

### DELETE /schedules/{scheduleId}
- 인증 필요. 응답 204 / 404: 없는·타 유저 일정
- usecase: `ScheduleWriter.delete`
