package com.chatbot.bravo.llm.openai;

import com.chatbot.bravo.infrastructure.llm.ToolParamExtractor;
import com.chatbot.bravo.model.llm.LlmMessage;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * ToolParamExtractor 의 OpenAI 구현. 툴 스펙 + 대화로 추출 프롬프트를 구성해
 * JSON 하나를 받고 요청 타입으로 매핑한다. 특정 툴 지식 없음 — 스펙은 호출자(툴)가 소유.
 */
@Slf4j
@Component
public class OpenAiToolParamExtractor implements ToolParamExtractor {

    private static final String MODEL = "gpt-4.1-mini";

    private static final String SYSTEM_TEMPLATE = """
            당신은 도구 호출 인자 추출기입니다. 아래 스펙에 따라 대화에서 필요한 값을 찾아
            JSON 객체 하나로만 응답하세요. JSON 외의 텍스트는 절대 포함하지 마세요.
            대화에 <system-context>로 주어진 오늘 날짜를 기준으로 상대 날짜(내일, 이번 주 등)를 해석하세요.

            %s""";

    private final ChatModel chatModel;
    private final ObjectMapper mapper;

    public OpenAiToolParamExtractor(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        // 모델이 스펙 밖 필드를 덧붙여도 매핑이 깨지지 않게 — 미지 필드 무시
        this.mapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public <T> T extract(String paramSpec, Class<T> type, List<LlmMessage> conversation) {
        List<Message> springMessages = new ArrayList<>();
        springMessages.add(new SystemMessage(SYSTEM_TEMPLATE.formatted(paramSpec)));
        for (LlmMessage m : conversation) {
            springMessages.add(switch (m.role()) {
                case ASSISTANT -> new AssistantMessage(m.content());
                case USER, TOOL -> new UserMessage(m.content());
            });
        }

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(MODEL)
                .temperature(0.0)   // 추출은 결정적으로
                .build();

        String text = chatModel.call(new Prompt(springMessages, options))
                .getResult().getOutput().getText();
        log.info("[OpenAI] 파라미터 추출 응답: type={}, length={}",
                type.getSimpleName(), text == null ? 0 : text.length());

        try {
            return mapper.readValue(CleanJson.cleanJsonString(text), type);
        } catch (Exception e) {
            throw new IllegalStateException("param extraction mapping failed: " + e.getMessage(), e);
        }
    }
}
