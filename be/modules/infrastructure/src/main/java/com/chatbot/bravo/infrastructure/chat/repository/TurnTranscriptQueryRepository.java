package com.chatbot.bravo.infrastructure.chat.repository;

import com.chatbot.bravo.model.chat.TranscriptEvent;

import java.util.List;

/**
 * 디버그 전문(transcript) 조회 전용 read-only Query 포트.
 * 상태·타입 필터 없이 유저의 전체 대화 이벤트를 시간순으로 반환한다.
 */
public interface TurnTranscriptQueryRepository {

    /**
     * 유저의 모든 Turn(PROCESSING/COMPLETED/FAILED 포함)의 모든 이벤트를
     * 시간순(turn id 오름차순 → event id 오름차순)으로 조회한다.
     */
    List<TranscriptEvent> findAllEvents(Long userId);
}
