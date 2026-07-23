package com.chatbot.bravo.service.chat.orchestrator;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class DefaultTurnContextInjector implements TurnContextInjector {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZONE);

    private static final String TURN_TEMPLATE = """
            <system-context>
            오늘 날짜는 %s입니다. 사용자의 시간대는 %s입니다.
            이 컨텍스트는 관련이 있을 수도, 없을 수도 있습니다. 사용자의 요청과 관련성이
            높지 않으면 여기에 직접 반응하지 마세요.
            </system-context>""";

    @Override
    public String buildTurnContext(Instant now) {
        return TURN_TEMPLATE.formatted(DATE.format(now), "Asia/Seoul");
    }
}
