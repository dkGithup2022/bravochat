package com.chatbot.bravo.service.chat.orchestrator.systemprompt;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

final class DefaultToolGuidanceContributor implements ToolGuidanceContributor {

    private static final String TOOL_GUIDANCE = """
            # 도구 사용

            주문, 배송, 결제에 관한 질문에 답하기 전에 항상 실제 데이터를 조회하세요 —
            고객이 말한 내용이나 당신의 기억에 의존하지 말고 조회 도구를 사용하세요.

            - 고객의 주문을 찾거나 상태를 확인하려면 주문 조회 도구를 사용하세요.
            - 무언가를 변경하는 작업(취소, 환불, 주소 변경)은 먼저 고객에게 해당 주문과
              변경 내용을 확인받은 뒤 실행 도구를 호출하세요. 성공한 뒤에는 무엇을 했는지
              분명히 알려주세요.
            - 한 질문에 답하기 위해 서로 독립적인 조회가 여러 개 필요하면, 하나씩이 아니라
              한꺼번에 요청하세요.

            도구가 오류를 반환하거나 결과가 없으면, 추측하지 말고 무엇을 찾았는지 고객에게
            알린 뒤 필요한 정보(주문 번호, 계정 이메일)를 요청하세요.""";

    @Override public String sectionName() { return "tool_guidance"; }
    @Override public Optional<PromptSection> contribute() { return contribute(new TreeSet<>()); }
    @Override public Optional<PromptSection> contribute(SortedSet<String> enabledTools) {
        if (enabledTools.isEmpty()) return Optional.empty();   // 도구 없으면 부재
        return Optional.of(new PromptSection("tool_guidance", TOOL_GUIDANCE, false));
    }
}
