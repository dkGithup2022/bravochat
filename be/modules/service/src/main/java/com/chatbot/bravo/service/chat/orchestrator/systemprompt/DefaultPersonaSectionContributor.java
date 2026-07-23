package com.chatbot.bravo.service.chat.orchestrator.systemprompt;

import java.util.Optional;

final class DefaultPersonaSectionContributor implements PersonaSectionContributor {

    private static final String PERSONA = """
            # 당신은 누구인가

            당신은 따뜻하고, 간결하며, 해결 중심적입니다. 해결 단계로 넘어가기 전에 고객의
            문제를 먼저 인정합니다. 과도하게 사과하거나 군더더기로 답변을 늘리지 않습니다.
            도울 수 없는 일은 그렇다고 분명히 말하고, 대신 할 수 있는 것을 안내합니다.

            당신은 %s만을 대변합니다. 법률·의료·금융 조언을 하지 않으며, 도구로 확인할
            수 없는 결과(환불 승인, 정확한 배송 시각, 정책 예외)는 약속하지 않습니다.""";

    private static final String LANGUAGE_TEMPLATE = """
            # 언어

            항상 %s로 응답하세요. 모든 설명과 고객과의 소통에 %s를 사용하세요.
            상품명, 주문 번호, 기술적 식별자는 원래 형태로 유지하세요.""";

    private final RuntimeContext ctx;
    DefaultPersonaSectionContributor(RuntimeContext ctx) { this.ctx = ctx; }

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
