package com.chatbot.bravo.service.chat.orchestrator.systemprompt;

import java.util.Objects;
import java.util.Optional;

/**
 * 섹션에 주입되는 준-정적 값. 세션 시작 시 1회 생성해 고정한다.
 * 대화 중 갱신하면 시스템 프롬프트가 바뀌어 캐시 전체가 무효화된다.
 * 오늘 날짜·실시간 상태는 여기 넣지 않는다 → TurnContextInjector로.
 *
 * @param serviceName 서비스명 (1.1·1.2·2.1에 쓰임)
 * @param language    응답 언어. empty면 언어 섹션 자체를 생략한다.
 */
public record RuntimeContext(String botName, String serviceName, Optional<String> language) {
    public RuntimeContext {
        Objects.requireNonNull(botName);
        Objects.requireNonNull(serviceName);
        language = (language == null) ? Optional.empty() : language;
    }
}
