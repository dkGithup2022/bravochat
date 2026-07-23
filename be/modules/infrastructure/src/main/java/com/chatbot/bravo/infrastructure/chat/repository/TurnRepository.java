package com.chatbot.bravo.infrastructure.chat.repository;

import com.chatbot.bravo.model.chat.Turn;

import java.util.Optional;

public interface TurnRepository {

    /** Turn 저장. 신규 생성(id=null) 및 상태 변경(COMPLETED/FAILED) 모두 이 메서드로 처리. */
    Turn save(Turn turn);

    Optional<Turn> findById(Long turnId);
}
