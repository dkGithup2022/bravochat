package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.model.llm.ToolInvocation;

/**
 * 툴 1개 = 이 인터페이스 구현 1개. (구현체는 아직 없음 — 계약만)
 * 실행하고, "대화에 남길 컨텍스트"({@link ToolOutcome#contextToLeave()})를 스스로 정의한다.
 */
public interface ToolHandler {

    /** 라우팅 키 + 시스템 프롬프트 목록 노출용 이름. */
    String name();

    /** 시스템 프롬프트 툴 목록에 노출할 설명. */
    String description();

    ToolOutcome handle(ToolInvocation call, ToolContext ctx);
}
