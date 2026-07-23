package com.chatbot.bravo.service.chat.orchestrator.systemprompt;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 시스템 프롬프트를 앱 시작 시 1회 조립해 제공한다. (package-private Default 구현들을 캡슐화)
 * MVP: 도구 없음(빈 enabledTools), RuntimeContext는 고정(bravo). 문구는 예시(주문봇) 기반 — 차후 교체.
 */
@Component
public class ChatSystemPromptProvider {

    // TODO: bravo 서비스에 맞는 RuntimeContext / 프롬프트 문구로 교체
    private static final RuntimeContext CTX =
            new RuntimeContext("bravo", "Bravo", Optional.of("한국어"));

    private final String systemPrompt;

    public ChatSystemPromptProvider() {
        SystemPromptBuilder builder = new DefaultSystemPromptBuilder(
                new DefaultCoreSectionContributor(CTX),
                new DefaultPersonaSectionContributor(CTX),
                new DefaultToolGuidanceContributor(),
                new DefaultStyleSectionContributor());
        this.systemPrompt = builder.build(CTX, new TreeSet<>()).stream()  // MVP: 도구 없음
                .map(PromptSection::text)
                .collect(Collectors.joining("\n\n"));
    }

    public String systemPrompt() {
        return systemPrompt;
    }
}
