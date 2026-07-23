# Infrastructure — Repository 포트 (현재 상태)

> add-infrastructure가 갱신. assess-infra/assess-usecase가 참조. 구현은 repository-jdbc.

## auth (com.chatbot.bravo.infrastructure.auth.repository)

### UserRepository
- `Optional<User> findByUsername(String username)`

### LoginSessionRepository
- `LoginSession save(LoginSession session)` — 신규 발급(id=null) + touch 갱신 공용
- `Optional<LoginSession> findBySessionKey(String sessionKey)` — 만료 판정은 호출자(SessionManager)
- `void deleteBySessionKey(String sessionKey)` — 로그아웃 soft-delete, 멱등

> 규약: 인터페이스만. 읽기→{domain} 반환, 쓰기→{domain} 반환. model/exception만 의존. 조회는 is_deleted=false 필터(JDBC 구현).

## chat (com.chatbot.bravo.infrastructure.chat.repository)

### RecentTurnQueryRepository (read-only 전용 Query 포트)
- `List<RecentTurn> findRecentCompletedTurns(Long userId, int size)` — 완료 Turn 최신순 최대 size개, USER+최종ASSISTANT agg(TOOL_* 제외), 반환은 오래된→최신
- 구현: `RecentTurnQueryJdbcRepository` (NamedParameterJdbcTemplate + 단순 조인 SQL)

### TurnRepository
- `Turn save(Turn)` — 신규 생성(id=null) + 상태변경 공용
- `Optional<Turn> findById(Long turnId)` — is_deleted=false 필터

### TurnEventRepository
- `TurnEvent append(TurnEvent)`, `List<TurnEvent> appendAll(List<TurnEvent>)`
- `List<TurnEvent> findAllByTurnIdOrderBySequence(Long turnId)` — is_deleted=false, seq 오름차순

> 구현체(TurnJdbcRepository/TurnEventJdbcRepository)는 준비됨. 현재 소비처는 SendMessage 오케스트레이션(스텁)이라 미사용 — 실제 저장/조회는 오케스트레이션 구현 시 검증.
