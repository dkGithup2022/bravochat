package com.chatbot.bravo.llm.openai;

public record GptRequest<T>(
        GptModel model,
        String systemPrompt,
        String userPrompt,
        Class<T> returnType,
        AdditionalOptions options
) {

    public static <T> GptRequest<T> of(GptModel model, String systemPrompt, String userPrompt, Class<T> returnType) {
        return new GptRequest<>(model, systemPrompt, userPrompt, returnType, null);
    }

    public static <T> GptRequest<T> of(GptModel model, String systemPrompt, String userPrompt, Class<T> returnType, AdditionalOptions options) {
        return new GptRequest<>(model, systemPrompt, userPrompt, returnType, options);
    }
}
