package com.chatbot.bravo.infrastructure.chat.repository;

import com.chatbot.bravo.model.chat.RecentTurn;

import java.util.List;

/**
 * 최근 대화 조회 전용 read-only Query 포트.
 * Turn/TurnEvent Repository 조합 대신 단일 조인 쿼리로 N+1을 회피한다.
 */
public interface RecentTurnQueryRepository {

    /**
     * 유저의 완료(COMPLETED) Turn을 최신순 최대 size개 조회하되,
     * 각 Turn의 USER_MESSAGE + ASSISTANT_MESSAGE만 agg한다. (TOOL_CALL/TOOL_RESULT 제외)
     * 반환 목록은 오래된→최신 순으로 정렬한다.
     */
    List<RecentTurn> findRecentCompletedTurns(Long userId, int size);
}
