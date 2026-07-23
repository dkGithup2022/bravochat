package com.chatbot.bravo.llm.openai;

/**
 * GPT 응답에서 JSON 문자열을 정리하는 유틸리티
 *
 * GPT가 markdown 코드 블록으로 감싸서 응답하는 경우 처리
 */
public final class CleanJson {

    private CleanJson() {}

    public static String cleanJsonString(String raw) {
        if (raw == null) return null;

        String trimmed = raw.trim();

        // ```json ... ``` 제거
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }
}
