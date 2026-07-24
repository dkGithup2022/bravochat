package com.chatbot.bravo.model.llm;

import java.util.Map;

/**
 * 모델이 요청한 툴 호출. (프롬프트 기반 — 네이티브 function-calling id 없음)
 *
 * @param name      호출할 툴 이름 (ToolCatalog에 등록된 이름과 일치해야 함)
 * @param arguments 툴 인자 (JSON object → Map). 각 툴 핸들러가 해석.
 */
public record ToolInvocation(
        String name,
        Map<String, Object> arguments
) {
}
