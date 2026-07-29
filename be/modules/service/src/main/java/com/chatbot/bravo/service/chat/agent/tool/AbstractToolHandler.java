package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.infrastructure.llm.ToolParamExtractor;
import com.chatbot.bravo.model.llm.ToolInvocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

/**
 * 파라미터 추출형 툴의 공통 뼈대(템플릿 메서드).
 * 흐름: 대화에서 인자 추출({@link ToolParamExtractor}) → {@link #doToolLogic}.
 *
 * <p>추출/실행 예외를 여기서 잡아 fail로 되먹인다 — 추출된 params를 아는 유일한 지점이라
 * turnMemo에 인풋·단계·에러·리턴을 래핑해 남길 수 있다. ToolExecutor의 예외 흡수는
 * 그 밖의 경우(미등록 툴 등)를 위한 최후 방어선으로 유지된다.
 *
 * <p>turnMemo는 JSON 한 건으로 기록된다:
 * {tool, stage(EXTRACT|EXECUTE), success, params, memo(툴 원본), error, returned(모델 되먹임 원문)}
 *
 * @param <P> 추출 결과 파라미터 타입 (툴이 정의)
 */
@Slf4j
public abstract class AbstractToolHandler<P> implements ToolHandler {

    private final ToolParamExtractor paramExtractor;
    private final ObjectMapper memoMapper;

    protected AbstractToolHandler(ToolParamExtractor paramExtractor, ObjectMapper objectMapper) {
        this.paramExtractor = paramExtractor;
        this.memoMapper = objectMapper;
    }

    @Override
    public final ToolResponse handle(ToolInvocation call, ToolContext ctx) {
        P params;
        try {
            params = paramExtractor.extract(paramSpec(), paramType(), ctx.conversation());
        } catch (RuntimeException e) {
            log.error("툴 파라미터 추출 실패: tool={}, turnId={}", name(), ctx.turnId(), e);
            String reason = "param extraction failed: " + e.getMessage();
            return new ToolResponse(reason, memo("EXTRACT", null, null, e, reason), false);
        }

        // 추출 결과를 중간 기록으로 즉시 저장 — 이후 실행이 죽어도 "뭘 뽑았는지"는 남는다
        ctx.progress(extractProgress(params));

        try {
            ToolResponse result = doToolLogic(params, ctx);
            return new ToolResponse(result.response(),
                    memo("EXECUTE", params, result, null, result.response()), result.success());
        } catch (RuntimeException e) {
            log.error("툴 실행 실패: tool={}, turnId={}", name(), ctx.turnId(), e);
            String reason = "tool execution error: " + e.getMessage();
            return new ToolResponse(reason, memo("EXECUTE", params, null, e, reason), false);
        }
    }

    /** TOOL_PROGRESS 기록용 — 추출 직후의 params를 JSON으로. */
    private String extractProgress(P params) {
        ObjectNode node = memoMapper.createObjectNode();
        node.put("stage", "EXTRACT");
        node.set("params", memoMapper.valueToTree(params));
        return node.toString();
    }

    /** turn_events(TOOL_RESULT) 기록 — 추출 인풋/시도 단계/에러/리턴을 JSON 한 건으로 래핑. */
    private String memo(String stage, P params, ToolResponse result, Exception error, String returned) {
        ObjectNode node = memoMapper.createObjectNode();
        node.put("tool", name());
        node.put("stage", stage);
        node.put("success", error == null && result != null && result.success());
        if (params != null) {
            node.set("params", memoMapper.valueToTree(params));
        }
        if (result != null) {
            node.put("memo", result.turnMemo());
        }
        if (error != null) {
            node.put("error", error.getClass().getSimpleName() + ": " + error.getMessage());
        }
        node.put("returned", returned);
        return node.toString();
    }

    /** 추출 콜에 줄 스펙 — 오퍼레이션 카탈로그 + 인자 설명 + 출력 JSON 형식 (툴이 소유). */
    protected abstract String paramSpec();

    protected abstract Class<P> paramType();

    /** 실제 툴 로직 — 추출된 파라미터로 작업하고 결과를 만든다. */
    protected abstract ToolResponse doToolLogic(P params, ToolContext ctx);
}
