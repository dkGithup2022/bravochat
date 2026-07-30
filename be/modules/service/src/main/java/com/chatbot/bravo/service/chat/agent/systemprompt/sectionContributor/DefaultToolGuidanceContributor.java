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
            알린 뒤 필요한 정보를 요청하세요.

            도구가 "확인 필요"를 요청하면 그 내용의 날짜·시각·제목 표기를 바꾸지 말고 그대로
            유저에게 보여주고 진행 여부를 확인받으세요. (다음 단계가 그 표기를 근거로 동작합니다.)

            도구가 관리하는 데이터(일정 등)에 대한 답변은 반드시 이번 턴의 도구 호출 결과에
            근거해야 합니다. 도구를 호출하지 않고 내용을 단정하거나 등록/변경을 "했다"고
            말하지 마세요. 이전 대화에 보이는 정보는 오래된 것일 수 있으므로, 브리핑 전에
            반드시 조회하세요.

            이전 대화의 [도구기록]은 그 시점에 실제 실행된 도구 결과입니다 — 무엇이 등록/변경
            됐는지 판단할 때 어시스턴트 발언이 아니라 [도구기록]을 근거로 삼으세요.

            유저가 직전 제안에 동의하면(예: "ㅇㅇ", "넵", "해줘"), 다시 확인하지 말고 즉시
            해당 도구를 호출해 실행하세요. 도구 호출 없이 "등록하겠습니다"라고 말하고 끝내는
            것은 금지입니다.

            한 요청에 등록/변경할 항목이 여러 건이면, 한 턴 안에서 도구를 건별로 연속 호출해
            전부 처리한 뒤 결과를 한 번에 정리하세요.

            특히 아래의 두 관련 작업은 유저의 요청 시, 먼저 요청 시점의 실제 데이터를
            조회한 후에 작업해주세요.
            - 기록, 일정""";

    @Override public String sectionName() { return "tool_guidance"; }
    @Override public Optional<PromptSection> contribute() { return contribute(new TreeSet<>()); }
    @Override public Optional<PromptSection> contribute(SortedSet<String> enabledTools) {
        if (enabledTools.isEmpty()) return Optional.empty();   // 도구 없으면 부재
        return Optional.of(new PromptSection("tool_guidance", TOOL_GUIDANCE, false));
    }
}
