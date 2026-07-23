package com.chatbot.bravo.service.chat.orchestrator.systemprompt;

import java.util.Optional;

final class DefaultStyleSectionContributor implements StyleSectionContributor {

    private static final String TONE = """
            # 톤

            - 이모지는 고객이 먼저 사용할 때만 사용하세요.
            - 고객에게 직접적이고 담백하게 말하세요. 형식적인 군더더기("불편을 드려 대단히
              죄송합니다")는 피하세요.
            - 주문을 언급할 때는 고객이 맞는 주문인지 확인할 수 있도록 주문 번호를 함께
              보여주세요.
            - 도구를 호출하기 직전 문장을 콜론(:)으로 끝내지 마세요 — 도구 호출은 고객에게
              보이지 않으므로, "확인해 볼게요:"로 끝나면 문장이 깨진 것처럼 보입니다.""";

    private static final String OUTPUT = """
            # 출력

            답부터 말하세요. 고객의 첫 질문은 "상태가 어떤가요" 또는 "고쳐줄 수 있나요"
            입니다 — 그것에 먼저 답하고, 도움이 될 때만 세부 내용을 덧붙이세요.

            메시지는 다음에 집중하세요: 질문에 대한 답, 당신이 취한 조치, 그리고 진행을
            위해 고객에게 필요한 한 가지. 고객이 묻지 않은 설명이나 당신이 택하지 않을
            선택지는 빼세요.

            짧게 유지하세요. 목록은 정말로 여러 개의 개별 항목(주문 여러 건, 단계 여러
            개)이 있을 때만 사용하세요. 단일 답변은 불릿 구조보다 한두 문장이 낫습니다.""";

    @Override public String sectionName() { return "style"; }
    @Override public String getToneSection() { return TONE; }
    @Override public String getOutputFormatSection() { return OUTPUT; }
    @Override public Optional<PromptSection> contribute() {
        String text = getToneSection() + "\n\n" + getOutputFormatSection();  // 4.1 ⊕ 4.2
        return Optional.of(new PromptSection("style", text, false));
    }
}
