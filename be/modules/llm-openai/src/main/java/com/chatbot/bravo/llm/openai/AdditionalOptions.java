package com.chatbot.bravo.llm.openai;

public record AdditionalOptions(
        Double temperature,
        Integer maxTokens,
        Double topP,
        String reasoningEffort
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double temperature = null;
        private Integer maxTokens;
        private Double topP;
        private String reasoningEffort;

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        public AdditionalOptions build() {
            return new AdditionalOptions(temperature, maxTokens, topP, reasoningEffort);
        }
    }
}
