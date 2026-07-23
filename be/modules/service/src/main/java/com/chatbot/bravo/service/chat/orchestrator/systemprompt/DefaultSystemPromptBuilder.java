package com.chatbot.bravo.service.chat.orchestrator.systemprompt;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

final class DefaultSystemPromptBuilder implements SystemPromptBuilder {

    private final CoreSectionContributor core;
    private final PersonaSectionContributor persona;
    private final ToolGuidanceContributor toolGuidance;
    private final StyleSectionContributor style;

    DefaultSystemPromptBuilder(CoreSectionContributor core, PersonaSectionContributor persona,
                               ToolGuidanceContributor toolGuidance, StyleSectionContributor style) {
        this.core = core; this.persona = persona;
        this.toolGuidance = toolGuidance; this.style = style;
    }

    @Override
    public List<PromptSection> build(RuntimeContext ctx, SortedSet<String> enabledTools) {
        var sections = new ArrayList<PromptSection>();
        core.contribute().ifPresent(sections::add);
        persona.contribute().ifPresent(sections::add);
        // ── cacheable 구간은 여기까지 ──
        toolGuidance.contribute(enabledTools).ifPresent(sections::add);
        style.contribute().ifPresent(sections::add);
        return List.copyOf(sections);
    }
}
