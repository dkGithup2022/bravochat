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
