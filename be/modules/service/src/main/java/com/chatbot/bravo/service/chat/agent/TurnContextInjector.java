package com.chatbot.bravo.service.chat.agent;

import java.time.Instant;

/** [프롬프트 밖] 매 턴 주입 — 시스템 프롬프트가 아니라 대화 메시지 끝에. */
public interface TurnContextInjector {
    String buildTurnContext(Instant now);
}
