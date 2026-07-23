package com.chatbot.bravo.llm.openai;

import java.util.List;

/**
 * OpenAI Chat API 클라이언트
 *
 * Spring AI ChatModel 기반, GPT 모델 호출 포트
 */
public interface OpenAiClient {

    <T> T callSingleType(GptRequest<T> request);

    <T> List<T> callListType(GptListRequest<T> request);
}
