package com.chatbot.bravo.llm.openai;

import com.chatbot.bravo.infrastructure.llm.LlmClient;
import com.chatbot.bravo.model.llm.LlmAction;
import com.chatbot.bravo.model.llm.LlmMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LlmClient 의 OpenAI(Spring AI ChatModel) 구현.
 * 메시지 리스트로 호출하고, 응답 텍스트를 {@link LlmAction}(FINAL | TOOL_CALL) JSON으로 파싱한다.
 * 파싱 실패 시(모델이 형식 안 지킴) 원문을 FINAL 답변으로 폴백한다.
 */
@Slf4j
@Component
public class OpenAiLlmClient implements LlmClient {

    private static final String MODEL = GptModel.FIVE_FOUR_MINI.getModelName();

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public OpenAiLlmClient(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmAction call(String systemPrompt, List<LlmMessage> messages) {
        List<Message> springMessages = new ArrayList<>();
        springMessages.add(new SystemMessage(systemPrompt));
        for (LlmMessage m : messages) {
            springMessages.add(switch (m.role()) {
                case ASSISTANT -> new AssistantMessage(m.content());
                case USER, TOOL -> new UserMessage(m.content());  // 프롬프트 기반 — TOOL도 user로 표현
            });
        }

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(MODEL)
                .temperature(1.0)
                // 프롬프트 지시만으로는 모델이 평문으로 답하는 경우가 있어 API 레벨에서 JSON 출력을 강제
                .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                .build();

        log.info("[OpenAI] 툴 루프 요청 전문:\n{}", PromptLog.render(springMessages));

        String text = chatModel.call(new Prompt(springMessages, options))
                .getResult().getOutput().getText();
        log.info("[OpenAI] 툴 루프 응답 전문:\n{}", text);

        return parse(text);
    }

    private LlmAction parse(String rawText) {
        String clean = CleanJson.cleanJsonString(rawText);
        try {
            return objectMapper.readValue(clean, LlmAction.class);
        } catch (Exception e) {
            // 모델이 JSON 형식을 안 지킨 경우 → 원문을 최종 답변으로 폴백
            log.warn("[OpenAI] LlmAction 파싱 실패 → FINAL 폴백: {}", e.getMessage());
            return LlmAction.finalAnswer(rawText);
        }
    }
}
