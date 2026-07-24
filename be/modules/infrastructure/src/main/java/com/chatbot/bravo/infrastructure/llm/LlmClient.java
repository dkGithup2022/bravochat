package com.chatbot.bravo.infrastructure.llm;

import com.chatbot.bravo.model.llm.LlmAction;
import com.chatbot.bravo.model.llm.LlmMessage;

import java.util.List;

/**
 * LLM 호출 포트 (provider 중립). 구현은 llm-openai 모듈의 OpenAiLlmClient.
 *
 * <p>매 호출은 구조화 응답 {@link LlmAction}(FINAL | TOOL_CALL)으로 파싱된다 —
 * 툴 루프의 한 스텝. systemPrompt에 응답 JSON 형식·툴 목록이 들어간다.
 */
public interface LlmClient {

    LlmAction call(String systemPrompt, List<LlmMessage> messages);
}
