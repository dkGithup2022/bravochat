package com.chatbot.bravo.infrastructure.llm;

import com.chatbot.bravo.model.llm.LlmMessage;

import java.util.List;

/**
 * 툴 파라미터 추출 포트 — 대화에서 툴 스펙에 맞는 인자를 뽑아 타입으로 매핑한다.
 * {@link LlmClient}(메인 루프용)와 별개의 호출. 스펙(paramSpec)의 내용은 각 툴이 소유하며,
 * 이 포트는 schedule 등 특정 툴을 모른다.
 */
public interface ToolParamExtractor {

    /**
     * 대화에서 paramSpec이 요구하는 값을 추출해 type으로 매핑한다.
     * 추출/매핑 실패는 RuntimeException — 호출 체인(ToolExecutor)이 fail로 흡수한다.
     */
    <T> T extract(String paramSpec, Class<T> type, List<LlmMessage> conversation);
}
