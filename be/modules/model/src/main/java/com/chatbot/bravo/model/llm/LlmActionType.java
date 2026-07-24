package com.chatbot.bravo.model.llm;

/** 프롬프트 기반 툴 루프에서 모델이 내리는 결정의 종류. */
public enum LlmActionType {
    /** 최종 답변 (툴 없이 종료). */
    FINAL,
    /** 툴 1개 호출 요청. */
    TOOL_CALL
}
