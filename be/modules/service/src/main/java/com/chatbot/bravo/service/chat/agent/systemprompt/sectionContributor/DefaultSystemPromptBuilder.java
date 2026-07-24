package com.chatbot.bravo.service.chat.agent.systemprompt.sectionContributor;

import com.chatbot.bravo.service.chat.agent.systemprompt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

public  final class DefaultSystemPromptBuilder implements SystemPromptBuilder {

    private final CoreSectionContributor core;
    private final PersonaSectionContributor persona;
    private final ToolGuidanceContributor toolGuidance;
    private final StyleSectionContributor style;
    private final ResponseFormatContributor responseFormat;

    public DefaultSystemPromptBuilder(CoreSectionContributor core, PersonaSectionContributor persona,
                                      ToolGuidanceContributor toolGuidance, StyleSectionContributor style,
                                      ResponseFormatContributor responseFormat) {
        this.core = core; this.persona = persona;
        this.toolGuidance = toolGuidance; this.style = style;
        this.responseFormat = responseFormat;
    }

    @Override
    public List<PromptSection> build(RuntimeContext ctx, SortedSet<String> enabledTools) {
        var sections = new ArrayList<PromptSection>();
        core.contribute().ifPresent(sections::add);
        persona.contribute().ifPresent(sections::add);
        // ── cacheable 구간은 여기까지 ──
        toolGuidance.contribute(enabledTools).ifPresent(sections::add);
        style.contribute().ifPresent(sections::add);
        responseFormat.contribute().ifPresent(sections::add);  // 응답 형식은 마지막 섹션
        return List.copyOf(sections);
    }
}
