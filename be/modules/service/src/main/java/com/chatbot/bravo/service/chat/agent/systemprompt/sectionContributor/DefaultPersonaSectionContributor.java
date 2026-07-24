package com.chatbot.bravo.service.chat.agent.systemprompt.sectionContributor;

import com.chatbot.bravo.service.chat.agent.systemprompt.PromptSection;
import com.chatbot.bravo.service.chat.agent.systemprompt.RuntimeContext;

import java.util.Optional;

public  final class DefaultPersonaSectionContributor implements PersonaSectionContributor {

    private static final String PERSONA = """
            # 당신은 누구인가

            당신은 %s의 어시스턴트로서 친근하면서도 간결하고, 도움 중심적입니다. 사용자의
            요청을 먼저 이해한 뒤 답하며, 과도하게 사과하거나 군더더기로 답을 늘리지 않습니다.
            도울 수 없는 일은 분명히 말하고, 대신 할 수 있는 것을 안내합니다.

            일상적인 대화를 자연스럽게 이어가는 것은 권장되지만, 억지로 유도할 필요는 없습니다.
            이전 대화의 내용과 분위기를 보고 판단하세요.

            확실하지 않은 사실은 단정하지 않고, 도구로 확인할 수 없는 것은 지어내지 않습니다.
            법률·의료·금융처럼 전문적 판단이 필요한 사안은 일반적인 정보 수준에서만 돕고,
            중요한 결정은 전문가 상담을 권합니다.""";

    private static final String LANGUAGE_TEMPLATE = """
            # 언어

            항상 %s로 응답하세요. 모든 설명과 사용자와의 소통에 %s를 사용하세요.
            고유명사, 코드, 기술 식별자는 원래 형태로 유지하세요.""";

    private final RuntimeContext ctx;
    public DefaultPersonaSectionContributor(RuntimeContext ctx) { this.ctx = ctx; }

    @Override public String sectionName() { return "persona"; }

    @Override public String getPersonaStatement() {   // 2.1
        return PERSONA.formatted(ctx.serviceName());
    }
    @Override public String getRuntimeContextSection(RuntimeContext c) {   // 2.2
        return c.language().map(l -> LANGUAGE_TEMPLATE.formatted(l, l)).orElse("");
    }
    @Override public Optional<PromptSection> contribute() {
        String rt = getRuntimeContextSection(ctx);
        String text = rt.isEmpty()
            ? getPersonaStatement()                     // 2.2 부재 → 2.1만
            : getPersonaStatement() + "\n\n" + rt;      // 2.1 ⊕ 2.2
        return Optional.of(new PromptSection("persona", text, true));
    }
}
