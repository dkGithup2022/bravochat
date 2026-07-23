package com.chatbot.bravo.llm.openai;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public record GptListRequest<T>(
        GptModel model,
        String systemPrompt,
        String userPrompt,
        TypeReference<List<T>> typeRef,
        AdditionalOptions options
) {

    public static <T> GptListRequest<T> of(GptModel model, String systemPrompt, String userPrompt, TypeReference<List<T>> typeRef) {
        return new GptListRequest<>(model, systemPrompt, userPrompt, typeRef, null);
    }

    public static <T> GptListRequest<T> of(GptModel model, String systemPrompt, String userPrompt, TypeReference<List<T>> typeRef, AdditionalOptions options) {
        return new GptListRequest<>(model, systemPrompt, userPrompt, typeRef, options);
    }
}
