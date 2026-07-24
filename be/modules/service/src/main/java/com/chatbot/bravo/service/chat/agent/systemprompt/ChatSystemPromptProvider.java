package com.chatbot.bravo.service.chat.agent.systemprompt;

import com.chatbot.bravo.service.chat.agent.systemprompt.sectionContributor.*;
import com.chatbot.bravo.service.chat.agent.tool.ToolCatalog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 시스템 프롬프트를 조립해 제공한다. (package-private Default 구현들을 캡슐화)
 *
 * <p>조립은 전적으로 {@link SystemPromptBuilder} 구성을 통한다:
 * 기본 섹션(Core/Persona/ToolGuidance/Style) + 툴 목록 + 응답 형식(LlmAction JSON)까지
 * 모두 {@link PromptSection}으로 다뤄 하나로 잇는다. 오케스트레이터는 문자열을 이어붙이지 않는다.
 *
 * <p>enabledTools를 인자로 받아 요청 시 조립한다(툴 유무가 유저별로 갈리므로).
 * MVP: RuntimeContext 고정(bravo), 문구는 예시(주문봇) 기반 — 차후 교체.
 */
@Component
public class ChatSystemPromptProvider {

    // TODO: bravo 서비스에 맞는 RuntimeContext / 프롬프트 문구로 교체
    private static final RuntimeContext CTX =
            new RuntimeContext("bravo", "Bravo", Optional.of("한국어"));

    private final SystemPromptBuilder builder;
    private final ToolCatalog toolCatalog;

    public ChatSystemPromptProvider(ToolCatalog toolCatalog) {
        this.toolCatalog = toolCatalog;
        this.builder = new DefaultSystemPromptBuilder(
                new DefaultCoreSectionContributor(CTX),
                new DefaultPersonaSectionContributor(CTX),
                new DefaultToolGuidanceContributor(),
                new DefaultStyleSectionContributor(),
                new DefaultResponseFormatContributor());
    }

    /**
     * enabledTools를 반영해 전체 시스템 프롬프트를 조립한다.
     * 기본 섹션 + 응답 형식은 빌더(컨트리뷰터)가, 툴 목록은 ToolCatalog가 — 전부 섹션으로 이어붙인다.
     */
    public String build(Set<String> enabledTools) {
        SortedSet<String> tools = new TreeSet<>(enabledTools);

        // 빌더 결과: [core, persona, (tool_guidance), style, response_format(마지막)]
        List<PromptSection> sections = new ArrayList<>(builder.build(CTX, tools));

        // 툴 목록(실제 스펙)은 tool_guidance("아래 스펙의 도구") 바로 뒤에 삽입.
        String toolList = toolCatalog.renderToolSection(enabledTools);
        if (!toolList.isBlank()) {
            int guidanceIdx = indexOfSection(sections, "tool_guidance");
            int insertAt = (guidanceIdx >= 0) ? guidanceIdx + 1 : sections.size() - 1;
            sections.add(insertAt, new PromptSection("tool_list", toolList, false));
        }

        return sections.stream()
                .map(PromptSection::text)
                .collect(Collectors.joining("\n\n"));
    }

    private static int indexOfSection(List<PromptSection> sections, String name) {
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
