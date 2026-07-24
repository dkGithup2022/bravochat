package com.chatbot.bravo.service.chat.agent.systemprompt.sectionContributor;

import com.chatbot.bravo.service.chat.agent.systemprompt.PromptSection;

import java.util.Optional;

public  final class DefaultStyleSectionContributor implements StyleSectionContributor {

    private static final String TONE = """
            # 톤

            - 이모지는 사용자가 먼저 사용할 때만 사용하세요.
            - 사용자에게 직접적이고 담백하게 말하세요. 형식적인 군더더기는 피하세요.
            - 도구를 호출하기 직전 문장을 콜론(:)으로 끝내지 마세요 — 도구 호출은 사용자에게
              보이지 않으므로, "확인해 볼게요:"로 끝나면 문장이 깨진 것처럼 보입니다.""";

    private static final String OUTPUT = """
            # 출력

            답부터 말하세요. 사용자가 물은 것에 먼저 답하고, 도움이 될 때만 세부 내용을
            덧붙이세요.

            메시지는 다음에 집중하세요: 질문에 대한 답, 당신이 취한 조치, 그리고 진행을
            위해 사용자에게 필요한 한 가지. 사용자가 묻지 않은 설명이나 당신이 택하지 않을
            선택지는 빼세요.

            짧게 유지하세요. 목록은 정말로 여러 개의 개별 항목이 있을 때만 사용하세요. 단일
            답변은 불릿 구조보다 한두 문장이 낫습니다.""";

    @Override public String sectionName() { return "style"; }
    @Override public String getToneSection() { return TONE; }
    @Override public String getOutputFormatSection() { return OUTPUT; }
    @Override public Optional<PromptSection> contribute() {
        String text = getToneSection() + "\n\n" + getOutputFormatSection();  // 4.1 ⊕ 4.2
        return Optional.of(new PromptSection("style", text, false));
    }
}
