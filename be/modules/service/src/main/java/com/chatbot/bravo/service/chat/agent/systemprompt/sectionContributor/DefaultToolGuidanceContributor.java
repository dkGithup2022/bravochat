package com.chatbot.bravo.service.chat.agent.systemprompt.sectionContributor;

import com.chatbot.bravo.service.chat.agent.systemprompt.PromptSection;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

public final class DefaultToolGuidanceContributor implements ToolGuidanceContributor {

    private static final String TOOL_GUIDANCE = """
            # 도구 사용

            요청 수행에 아래 스펙의 도구 중 하나가 필요한 경우, 도구를 사용하세요. 확신이
            들지 않는다면 유저에게 한번 물어봐도 됩니다. (이전 대화도 다음 대화에 포함되기
            때문에 확인은 괜찮습니다.)

            - 한 요청에 서로 독립적인 조회가 여러 개 필요하면, 하나씩이 아니라 한꺼번에
              요청하세요.

            도구가 오류를 반환하거나 결과가 없으면, 추측하지 말고 무엇을 확인했는지 사용자에게
            알린 뒤 필요한 정보를 요청하세요.""";

    @Override public String sectionName() { return "tool_guidance"; }
    @Override public Optional<PromptSection> contribute() { return contribute(new TreeSet<>()); }
    @Override public Optional<PromptSection> contribute(SortedSet<String> enabledTools) {
        if (enabledTools.isEmpty()) return Optional.empty();   // 도구 없으면 부재
        return Optional.of(new PromptSection("tool_guidance", TOOL_GUIDANCE, false));
    }
}
