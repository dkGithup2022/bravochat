-- schema.sql (MySQL 8 — prod)
-- local_h2/schema.sql 과 동일 구조의 MySQL 버전. 자동 실행되지 않음(sql.init.mode: never) —
-- 운영 DB 에 수동 적용한다. local_h2 에 도메인이 추가되면 여기에도 함께 반영할 것.
-- 변환 규칙: CLOB→MEDIUMTEXT, 인덱스는 테이블 인라인(MySQL 은 CREATE INDEX IF NOT EXISTS 미지원),
--            H2 전용 마이그레이션 라인 제거, utf8mb4(한글) 명시.
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    CONSTRAINT uk_login_sessions_session_key UNIQUE (session_key),
    INDEX idx_login_sessions_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    updated_at     DATETIME(6) NOT NULL,
    INDEX idx_turns_user_status_created (user_id, status, created_at DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 이벤트 순서는 id(AUTO_INCREMENT) 오름차순으로 보장 — 별도 sequence 컬럼 없음.
CREATE TABLE IF NOT EXISTS turn_events (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    turn_id      BIGINT      NOT NULL,
    type         VARCHAR(30) NOT NULL,
    content      MEDIUMTEXT  NOT NULL,
    tool_name    VARCHAR(100),
    tool_call_id VARCHAR(100),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at   DATETIME(6),
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    INDEX idx_turn_events_turn_type (turn_id, type),
    INDEX idx_turn_events_tool_call (tool_call_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    updated_at    DATETIME(6)  NOT NULL,
    INDEX idx_schedules_user_scheduled (user_id, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- === 유저 등록 (운영자 수동 실행 예시 — 이 파일에 실제 값을 커밋하지 말 것) ===
-- BCrypt 해시 생성: htpasswd -bnBC 10 "" '<비밀번호>' | tr -d ':\n'
-- INSERT INTO users (username, password_hash, is_deleted, created_at, updated_at)
-- VALUES ('<username>', '<bcrypt-hash>', FALSE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
