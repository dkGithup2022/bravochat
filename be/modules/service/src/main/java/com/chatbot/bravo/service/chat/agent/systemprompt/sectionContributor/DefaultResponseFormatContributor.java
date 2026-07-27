package com.chatbot.bravo.service.chat.agent.systemprompt.sectionContributor;

import com.chatbot.bravo.service.chat.agent.systemprompt.PromptSection;

import java.util.Optional;

public final class DefaultResponseFormatContributor implements ResponseFormatContributor {

    private static final String RESPONSE_FORMAT = """
            # 응답 형식

            반드시 아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 절대 포함하지 마세요.
            - 최종 답변일 때: {"type":"FINAL","content":"<사용자에게 보여줄 답변>"}
            - 툴을 호출할 때: {"type":"TOOL_CALL","tool":{"name":"<툴 이름>","arguments":{}}}
            툴 호출 시 인자는 채우지 마세요 — 도구가 대화를 보고 스스로 파악합니다.
            사용 가능한 툴이 없으면 항상 FINAL 로 답하세요.""";

    @Override public String sectionName() { return "response_format"; }

    @Override public String getResponseFormatSection() { return RESPONSE_FORMAT; }

    @Override public Optional<PromptSection> contribute() {
        return Optional.of(new PromptSection("response_format", RESPONSE_FORMAT, false));
    }
}
