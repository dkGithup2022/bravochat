package com.chatbot.bravo.jdbc.chat.repository;

import com.chatbot.bravo.infrastructure.chat.repository.TurnTranscriptQueryRepository;
import com.chatbot.bravo.model.chat.TranscriptEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class TurnTranscriptQueryJdbcRepository implements TurnTranscriptQueryRepository {

    // 디버그 전문 조회 — 상태 필터 없음(PROCESSING/FAILED 포함), 이벤트 순서는 id 오름차순.
    private static final String SQL = """
            SELECT t.id           AS turn_id,
                   t.status       AS turn_status,
                   e.type         AS event_type,
                   e.tool_name    AS tool_name,
                   e.content      AS content,
                   e.created_at   AS created_at
            FROM turns t
            JOIN turn_events e
                 ON e.turn_id = t.id AND e.is_deleted = FALSE
            WHERE t.user_id = :userId
              AND t.is_deleted = FALSE
            ORDER BY t.id ASC, e.id ASC
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<TranscriptEvent> findAllEvents(Long userId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);

        return jdbc.query(SQL, params, (rs, rowNum) -> new TranscriptEvent(
                rs.getLong("turn_id"),
                rs.getString("turn_status"),
                rs.getString("event_type"),
                rs.getString("tool_name"),
                rs.getString("content"),
                rs.getTimestamp("created_at").toInstant()));
    }
}
