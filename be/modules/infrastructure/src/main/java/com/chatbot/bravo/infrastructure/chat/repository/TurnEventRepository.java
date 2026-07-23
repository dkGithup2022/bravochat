package com.chatbot.bravo.infrastructure.chat.repository;

import com.chatbot.bravo.model.chat.TurnEvent;

import java.util.List;

public interface TurnEventRepository {

    /** 이벤트 단건 append. */
    TurnEvent append(TurnEvent event);

    /** 이벤트 다건 append (한 번의 LLM 왕복에서 발생한 TOOL_CALL/TOOL_RESULT 묶음 등). */
    List<TurnEvent> appendAll(List<TurnEvent> events);

    /** Turn의 이벤트를 sequence 오름차순으로 조회. */
    List<TurnEvent> findAllByTurnIdOrderBySequence(Long turnId);
}
