-- schema.sql (H2 — local/test)
-- 각 도메인 DDL 은 add-entity-specs 스킬이 이 아래에 append 한다. (블록: -- === {Domain} ===)
-- 시각 컬럼 규약: DATETIME(6) + UTC 저장 (Application.main 이 JVM 타임존 고정)
-- FK 제약 미사용 — 참조 무결성은 애플리케이션 레벨 관리

-- === Auth ===
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    DATETIME(6),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS login_sessions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_key       VARCHAR(36)  NOT NULL,
    user_id           BIGINT       NOT NULL,
    last_logged_in_at DATETIME(6)  NOT NULL,
    last_requested_at DATETIME(6)  NOT NULL,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at        DATETIME(6),
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    CONSTRAINT uk_login_sessions_session_key UNIQUE (session_key)
);

CREATE INDEX IF NOT EXISTS idx_login_sessions_user_id ON login_sessions(user_id);

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

-- 이벤트 순서는 id(AUTO_INCREMENT) 오름차순으로 보장 — 별도 sequence 컬럼 없음.
CREATE TABLE IF NOT EXISTS turn_events (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    turn_id      BIGINT      NOT NULL,
    type         VARCHAR(30) NOT NULL,
    content      CLOB        NOT NULL,
    tool_name    VARCHAR(100),
    tool_call_id VARCHAR(100),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at   DATETIME(6),
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_turn_events_turn_type ON turn_events(turn_id, type);
CREATE INDEX IF NOT EXISTS idx_turn_events_tool_call ON turn_events(tool_call_id);

-- === Schedule ===
-- 챗 툴 또는 일정 API로 생성 — turn_id 로 생성 출처 턴 추적 (NULL = API 발 생성).
-- done_at NULL = 미완료 (별도 boolean 없음)
CREATE TABLE IF NOT EXISTS schedules (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    turn_id       BIGINT,
    title         VARCHAR(200) NOT NULL,
    content       TEXT,
    schedule_type VARCHAR(20)  NOT NULL,
    scheduled_at  DATETIME(6)  NOT NULL,
    done_at       DATETIME(6),
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    DATETIME(6),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL
);
-- 기존 로컬 DB 마이그레이션 — 이미 nullable이면 no-op (idempotent)
ALTER TABLE schedules ALTER COLUMN turn_id SET NULL;
CREATE INDEX IF NOT EXISTS idx_schedules_user_scheduled ON schedules(user_id, scheduled_at);
