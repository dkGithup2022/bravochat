package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.model.llm.ToolInvocation;

/**
 * 툴 1개 = 이 인터페이스 구현 1개. 툴은 블랙박스 —
 * 외부(루프/매니저/익스큐터)는 name/promptText만 알고,
 * 내부 동작 선택·인자 해석은 툴이 {@link ToolContext#conversation()}을 보고 스스로 한다.
 */
public interface ToolHandler {

    /** 라우팅 키 + 시스템 프롬프트 툴 목록 노출용 이름. */
    String name();

    /** 시스템 프롬프트 툴 목록에 들어갈 설명 텍스트 — 툴이 소유하며, 외부가 아는 전부. */
    String promptText();

    ToolResponse handle(ToolInvocation call, ToolContext ctx);
}
