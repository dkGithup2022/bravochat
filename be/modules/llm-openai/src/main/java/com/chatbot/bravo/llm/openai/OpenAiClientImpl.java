package com.chatbot.bravo.llm.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class OpenAiClientImpl implements OpenAiClient {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final ObjectMapper tolerantMapper;

    public OpenAiClientImpl(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;

        this.tolerantMapper = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T callSingleType(GptRequest<T> request) {
        Prompt prompt = buildPrompt(
                request.systemPrompt(),
                request.userPrompt(),
                request.model().getModelName(),
                request.options()
        );

        Generation generation = chatModel.call(prompt).getResult();
        String raw = CleanJson.cleanJsonString(generation.getOutput().getText());
        log.info("[OpenAI] 응답 수신: model={}, length={}", request.model().getModelName(), raw.length());

        try {
            if (request.returnType().equals(String.class)) {
                return (T) raw;
            }
            return safeObjectMapper(raw, request.returnType());
        } catch (RuntimeException | JsonProcessingException e) {
            log.error("[OpenAI] JSON 파싱 오류 - raw: {}", raw, e);
            throw new RuntimeException("JSON 파싱 오류", e);
        }
    }

    @Override
    public <T> List<T> callListType(GptListRequest<T> request) {
        Prompt prompt = buildPrompt(
                request.systemPrompt(),
                request.userPrompt(),
                request.model().getModelName(),
                request.options()
        );

        Generation generation = chatModel.call(prompt).getResult();
        String raw = CleanJson.cleanJsonString(generation.getOutput().getText());
        log.info("[OpenAI] 리스트 응답 수신: model={}, length={}", request.model().getModelName(), raw.length());

        try {
            return objectMapper.readValue(raw, request.typeRef());
        } catch (JsonProcessingException e) {
            log.error("[OpenAI] 리스트 JSON 파싱 오류 - raw: {}", raw, e);
            throw new RuntimeException("리스트 JSON 파싱 오류", e);
        }
    }

    private Prompt buildPrompt(String systemPrompt, String userPrompt, String model, AdditionalOptions options) {
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(model)
                .streamUsage(false)
                .temperature(1.0);

        if (options != null) {
            if (options.temperature() != null) {
                optionsBuilder.temperature(options.temperature());
            }
            if (options.maxTokens() != null) {
                optionsBuilder.maxTokens(options.maxTokens());
            }
            if (options.topP() != null) {
                optionsBuilder.topP(options.topP());
            }
            if (options.reasoningEffort() != null) {
                optionsBuilder.reasoningEffort(options.reasoningEffort());
            }
        }

        OpenAiChatOptions chatOptions = optionsBuilder.build();

        return new Prompt(
                List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userPrompt)
                ),
                chatOptions
        );
    }

    private <T> T safeObjectMapper(String raw, Class<T> returnType) throws JsonProcessingException {
        try {
            return objectMapper.readValue(raw, returnType);
        } catch (JsonProcessingException e) {
            log.warn("[OpenAI] 기본 ObjectMapper 파싱 실패, tolerantMapper로 재시도");
            return tolerantMapper.readValue(raw, returnType);
        }
    }
}
