-- data.sql (H2 — local 시드)
-- 각 도메인 시드 INSERT 는 이 아래에 append 한다.

-- === Auth ===
-- 로그인 검증용 테스트 유저. password_hash = BCrypt("password1234")
INSERT INTO users (username, password_hash, is_deleted, created_at, updated_at)
SELECT 'tester', '$2a$10$3cgyvfPAXtijkNKB0hckIei7BqwvAK9RF4IVlz6w0/rR3yz3MuxPS', FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'tester');

-- 두 번째 테스트 유저. password_hash = BCrypt("password1234") (tester와 동일 비번)
INSERT INTO users (username, password_hash, is_deleted, created_at, updated_at)
SELECT 'tester2', '$2a$10$3cgyvfPAXtijkNKB0hckIei7BqwvAK9RF4IVlz6w0/rR3yz3MuxPS', FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'tester2');

-- === Chat ===
-- tester(user_id=1)의 대화 시드. 이벤트는 Turn의 고유 created_at으로 링크(AUTO_INCREMENT 충돌 회피).
-- 이벤트 순서는 id 오름차순 = 삽입 순서. 멱등 가드는 (turn_id, type) 기준(시드 턴은 type당 이벤트 1개).
-- Turn A: 일반 대화 / Turn B: 툴 호출 포함(결과에서 제외 검증) / Turn C: 일반 / Turn D: PROCESSING(제외)
INSERT INTO turns (user_id, status, completed_at, is_deleted, created_at, updated_at)
SELECT 1, 'COMPLETED', TIMESTAMP '2026-07-20 01:00:05', FALSE, TIMESTAMP '2026-07-20 01:00:00', TIMESTAMP '2026-07-20 01:00:05'
WHERE NOT EXISTS (SELECT 1 FROM turns WHERE user_id = 1 AND created_at = TIMESTAMP '2026-07-20 01:00:00');
INSERT INTO turns (user_id, status, completed_at, is_deleted, created_at, updated_at)
SELECT 1, 'COMPLETED', TIMESTAMP '2026-07-20 01:05:08', FALSE, TIMESTAMP '2026-07-20 01:05:00', TIMESTAMP '2026-07-20 01:05:08'
WHERE NOT EXISTS (SELECT 1 FROM turns WHERE user_id = 1 AND created_at = TIMESTAMP '2026-07-20 01:05:00');
INSERT INTO turns (user_id, status, completed_at, is_deleted, created_at, updated_at)
SELECT 1, 'COMPLETED', TIMESTAMP '2026-07-20 01:10:03', FALSE, TIMESTAMP '2026-07-20 01:10:00', TIMESTAMP '2026-07-20 01:10:03'
WHERE NOT EXISTS (SELECT 1 FROM turns WHERE user_id = 1 AND created_at = TIMESTAMP '2026-07-20 01:10:00');
INSERT INTO turns (user_id, status, is_deleted, created_at, updated_at)
SELECT 1, 'PROCESSING', FALSE, TIMESTAMP '2026-07-20 01:15:00', TIMESTAMP '2026-07-20 01:15:00'
WHERE NOT EXISTS (SELECT 1 FROM turns WHERE user_id = 1 AND created_at = TIMESTAMP '2026-07-20 01:15:00');

-- Turn A 이벤트
INSERT INTO turn_events (turn_id, type, content, is_deleted, created_at, updated_at)
SELECT t.id, 'USER_MESSAGE', '안녕하세요', FALSE, t.created_at, t.created_at
FROM turns t WHERE t.user_id = 1 AND t.created_at = TIMESTAMP '2026-07-20 01:00:00'
  AND NOT EXISTS (SELECT 1 FROM turn_events e WHERE e.turn_id = t.id AND e.type = 'USER_MESSAGE');
INSERT INTO turn_events (turn_id, type, content, is_deleted, created_at, updated_at)
SELECT t.id, 'ASSISTANT_MESSAGE', '안녕하세요. 무엇을 도와드릴까요?', FALSE, t.created_at, t.created_at
FROM turns t WHERE t.user_id = 1 AND t.created_at = TIMESTAMP '2026-07-20 01:00:00'
  AND NOT EXISTS (SELECT 1 FROM turn_events e WHERE e.turn_id = t.id AND e.type = 'ASSISTANT_MESSAGE');

-- Turn B 이벤트 (USER → TOOL_CALL → TOOL_RESULT → ASSISTANT). id 순서 = 삽입 순서.
INSERT INTO turn_events (turn_id, type, content, is_deleted, created_at, updated_at)
SELECT t.id, 'USER_MESSAGE', '오늘 서울 날씨 알려줘', FALSE, t.created_at, t.created_at
FROM turns t WHERE t.user_id = 1 AND t.created_at = TIMESTAMP '2026-07-20 01:05:00'
  AND NOT EXISTS (SELECT 1 FROM turn_events e WHERE e.turn_id = t.id AND e.type = 'USER_MESSAGE');
INSERT INTO turn_events (turn_id, type, content, tool_name, tool_call_id, is_deleted, created_at, updated_at)
SELECT t.id, 'TOOL_CALL', '{"city":"서울"}', 'get_weather', 'call_1', FALSE, t.created_at, t.created_at
FROM turns t WHERE t.user_id = 1 AND t.created_at = TIMESTAMP '2026-07-20 01:05:00'
  AND NOT EXISTS (SELECT 1 FROM turn_events e WHERE e.turn_id = t.id AND e.type = 'TOOL_CALL');
INSERT INTO turn_events (turn_id, type, content, tool_call_id, is_deleted, created_at, updated_at)
SELECT t.id, 'TOOL_RESULT', '{"temp":25,"sky":"맑음"}', 'call_1', FALSE, t.created_at, t.created_at
FROM turns t WHERE t.user_id = 1 AND t.created_at = TIMESTAMP '2026-07-20 01:05:00'
  AND NOT EXISTS (SELECT 1 FROM turn_events e WHERE e.turn_id = t.id AND e.type = 'TOOL_RESULT');
INSERT INTO turn_events (turn_id, type, content, is_deleted, created_at, updated_at)
SELECT t.id, 'ASSISTANT_MESSAGE', '오늘 서울은 맑고 25도입니다.', FALSE, t.created_at, t.created_at
FROM turns t WHERE t.user_id = 1 AND t.created_at = TIMESTAMP '2026-07-20 01:05:00'
  AND NOT EXISTS (SELECT 1 FROM turn_events e WHERE e.turn_id = t.id AND e.type = 'ASSISTANT_MESSAGE');

-- Turn C 이벤트
INSERT INTO turn_events (turn_id, type, content, is_deleted, created_at, updated_at)
SELECT t.id, 'USER_MESSAGE', '고마워', FALSE, t.created_at, t.created_at
FROM turns t WHERE t.user_id = 1 AND t.created_at = TIMESTAMP '2026-07-20 01:10:00'
  AND NOT EXISTS (SELECT 1 FROM turn_events e WHERE e.turn_id = t.id AND e.type = 'USER_MESSAGE');
INSERT INTO turn_events (turn_id, type, content, is_deleted, created_at, updated_at)
SELECT t.id, 'ASSISTANT_MESSAGE', '천만에요!', FALSE, t.created_at, t.created_at
FROM turns t WHERE t.user_id = 1 AND t.created_at = TIMESTAMP '2026-07-20 01:10:00'
  AND NOT EXISTS (SELECT 1 FROM turn_events e WHERE e.turn_id = t.id AND e.type = 'ASSISTANT_MESSAGE');

-- Turn D 이벤트 (PROCESSING — USER만, 결과에서 제외)
INSERT INTO turn_events (turn_id, type, content, is_deleted, created_at, updated_at)
SELECT t.id, 'USER_MESSAGE', '처리 중인 질문', FALSE, t.created_at, t.created_at
FROM turns t WHERE t.user_id = 1 AND t.created_at = TIMESTAMP '2026-07-20 01:15:00'
  AND NOT EXISTS (SELECT 1 FROM turn_events e WHERE e.turn_id = t.id AND e.type = 'USER_MESSAGE');
