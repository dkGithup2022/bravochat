package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.infrastructure.llm.ToolParamExtractor;
import com.chatbot.bravo.model.llm.ToolInvocation;

/**
 * 파라미터 추출형 툴의 공통 뼈대(템플릿 메서드).
 * 흐름: 대화에서 인자 추출({@link ToolParamExtractor}) → {@link #doToolLogic}.
 *
 * <p>추출 실패(LLM 호출/매핑 예외)는 여기서 잡지 않는다 —
 * ToolExecutor(관문)의 예외 흡수가 fail로 되먹인다.
 *
 * @param <P> 추출 결과 파라미터 타입 (툴이 정의)
 */
public abstract class AbstractToolHandler<P> implements ToolHandler {

    private final ToolParamExtractor paramExtractor;

    protected AbstractToolHandler(ToolParamExtractor paramExtractor) {
        this.paramExtractor = paramExtractor;
    }

    @Override
    public final ToolResponse handle(ToolInvocation call, ToolContext ctx) {
        P params = paramExtractor.extract(paramSpec(), paramType(), ctx.conversation());
        return doToolLogic(params, ctx);
    }

    /** 추출 콜에 줄 스펙 — 오퍼레이션 카탈로그 + 인자 설명 + 출력 JSON 형식 (툴이 소유). */
    protected abstract String paramSpec();

    protected abstract Class<P> paramType();

    /** 실제 툴 로직 — 추출된 파라미터로 작업하고 결과를 만든다. */
    protected abstract ToolResponse doToolLogic(P params, ToolContext ctx);
}
