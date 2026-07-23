package com.chatbot.bravo.llm.openai;

import com.chatbot.bravo.infrastructure.llm.LlmClient;
import com.chatbot.bravo.model.llm.LlmMessage;
import com.chatbot.bravo.model.llm.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LlmClient 의 OpenAI(Spring AI ChatModel) 구현. MVP: 도구 없이 systemPrompt + messages 단발 호출.
 * Phase 2: OpenAiChatOptions 에 toolCallbacks 추가 + 응답 툴 콜 파싱.
 */
@Slf4j
@Component
public class OpenAiLlmClient implements LlmClient {

    private static final String MODEL = "gpt-4.1-mini";

    private final ChatModel chatModel;

    public OpenAiLlmClient(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public LlmResponse call(String systemPrompt, List<LlmMessage> messages) {
        List<Message> springMessages = new ArrayList<>();
        springMessages.add(new SystemMessage(systemPrompt));
        for (LlmMessage m : messages) {
            springMessages.add(switch (m.role()) {
                case ASSISTANT -> new AssistantMessage(m.content());
                case USER -> new UserMessage(m.content());
            });
        }

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(MODEL)
                .temperature(1.0)
                .build();

        String text = chatModel.call(new Prompt(springMessages, options))
                .getResult().getOutput().getText();
        log.info("[OpenAI] 응답 수신: length={}", text == null ? 0 : text.length());

        return LlmResponse.finalText(text);
    }
}
