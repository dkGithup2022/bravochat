package com.chatbot.bravo.infrastructure.llm;

import com.chatbot.bravo.model.llm.LlmMessage;
import com.chatbot.bravo.model.llm.LlmResponse;

import java.util.List;

/**
 * LLM 호출 포트 (provider 중립). 구현은 llm-openai 모듈의 OpenAiLlmClient.
 * MVP: 도구 없이 systemPrompt + messages로 단발 호출.
 * Phase 2: tools 파라미터 + 응답의 toolCalls 로 확장.
 */
public interface LlmClient {

    LlmResponse call(String systemPrompt, List<LlmMessage> messages);
}
