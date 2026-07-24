package com.chatbot.bravo.service.chat.agent.memory;

import com.chatbot.bravo.model.llm.LlmMessage;

import java.util.List;

/**
 * 대화 메모리 로딩 계약. LLM 입력에 넣을 "이전 대화"를 어떤 전략으로 구성할지 담당한다.
 *
 * <p>지금은 {@link #recentTurns}(최근 완료 대화) 하나뿐이지만, 확장 지점:
 * 토큰 예산 기반 슬라이딩, 오래된 대화 요약 압축, 의미 검색(RAG) 등을 이 매니저에 메서드로 붙인다.
 */
public interface MemoryManager {

    /** 유저의 최근 완료 대화를 LLM 메시지로 반환한다 (오래된→최신, USER/ASSISTANT). */
    List<LlmMessage> recentTurns(Long userId);
}
