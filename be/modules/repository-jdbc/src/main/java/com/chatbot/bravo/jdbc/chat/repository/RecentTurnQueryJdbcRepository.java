package com.chatbot.bravo.jdbc.chat.repository;

import com.chatbot.bravo.infrastructure.chat.repository.RecentTurnQueryRepository;
import com.chatbot.bravo.model.chat.RecentTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
class RecentTurnQueryJdbcRepository implements RecentTurnQueryRepository {

    // 완료 Turn당 USER_MESSAGE·ASSISTANT_MESSAGE 각 1개 불변식(도메인 §7)에 기대어
    // 단순 조인으로 agg. TOOL_CALL/TOOL_RESULT는 type 필터로 자연 배제.
    // 최신순 LIMIT 후 앱에서 reverse → 오래된→최신.
    private static final String SQL = """
            SELECT t.id                AS turn_id,
                   u.content           AS user_message,
                   a.content           AS assistant_message,
                   t.created_at        AS created_at
            FROM turns t
            JOIN turn_events u
                 ON u.turn_id = t.id AND u.type = 'USER_MESSAGE'      AND u.is_deleted = FALSE
            JOIN turn_events a
                 ON a.turn_id = t.id AND a.type = 'ASSISTANT_MESSAGE' AND a.is_deleted = FALSE
            WHERE t.user_id = :userId
              AND t.status = 'COMPLETED'
              AND t.is_deleted = FALSE
            ORDER BY t.created_at DESC, t.id DESC
            LIMIT :size
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<RecentTurn> findRecentCompletedTurns(Long userId, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("size", size);

        List<RecentTurn> rows = jdbc.query(SQL, params, (rs, rowNum) -> new RecentTurn(
                rs.getLong("turn_id"),
                rs.getString("user_message"),
                rs.getString("assistant_message"),
                rs.getTimestamp("created_at").toInstant()));

        Collections.reverse(rows); // 최신순 → 오래된→최신
        return rows;
    }
}
