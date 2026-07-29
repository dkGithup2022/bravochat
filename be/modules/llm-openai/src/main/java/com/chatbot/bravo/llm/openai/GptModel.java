package com.chatbot.bravo.llm.openai;

public enum GptModel {
    FOUR_NANO("gpt-4.1-nano"),
    FOUR_MINI("gpt-4.1-mini"),
    FOUR_NORMAL("gpt-4.1"),

    FIVE_NANO("gpt-5.1-nano"),
    FIVE_MINI("gpt-5-mini-2025-08-07"),
    FIVE_NORMAL("gpt-5.1-2025-11-13"),

    FIVE_FOUR_MINI("gpt-5.4-mini-2026-03-17");

    private final String modelName;

    GptModel(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}
