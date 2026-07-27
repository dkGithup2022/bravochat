package com.chatbot.bravo.service.chat.agent;

import com.chatbot.bravo.exception.chat.LlmExecutionException;
import com.chatbot.bravo.infrastructure.chat.repository.TurnEventRepository;
import com.chatbot.bravo.infrastructure.chat.repository.TurnRepository;
import com.chatbot.bravo.infrastructure.llm.LlmClient;
import com.chatbot.bravo.model.chat.Turn;
import com.chatbot.bravo.model.chat.TurnEvent;
import com.chatbot.bravo.model.llm.LlmAction;
import com.chatbot.bravo.model.llm.LlmMessage;
import com.chatbot.bravo.model.llm.ToolInvocation;
import com.chatbot.bravo.service.chat.SendMessageUsecase;
import com.chatbot.bravo.service.chat.dto.SendMessageCommand;
import com.chatbot.bravo.service.chat.dto.SendMessageResult;
import com.chatbot.bravo.service.chat.agent.memory.MemoryManager;
import com.chatbot.bravo.service.chat.agent.systemprompt.ChatSystemPromptProvider;
import com.chatbot.bravo.service.chat.agent.tool.ToolContext;
import com.chatbot.bravo.service.chat.agent.tool.ToolExecutor;
import com.chatbot.bravo.service.chat.agent.tool.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SendMessage 오케스트레이션 — 프롬프트 기반 툴 루프.
 *
 * <p>{@link #sendMessage}가 "인증 → 컨텍스트 조립 → 대화 시작 기록 → 루프 → 완료" 흐름만 드러낸다.
 * 루프는 매 호출을 {@link LlmAction}(FINAL | TOOL_CALL)으로 파싱한다:
 * FINAL이면 종료(①), TOOL_CALL이면 툴 실행 후 결과를 messages에 append하고 재호출.
 * 종료는 ①모델 FINAL / ②에러(fail) / ③MAX_STEPS 가드.
 *
 * <p>활성 툴은 EnabledToolsResolver가 결정(등록된 ToolHandler 전부). 등록 툴이 없으면
 * 모델은 항상 FINAL → 루프 1바퀴. 툴은 블랙박스 — 루프는 name 라우팅과 결과 기록만 안다.
 *
 * <p>주의: 메서드에 @Transactional을 걸지 않는다 — LLM 호출(느린 외부 I/O) 동안 DB 커넥션을
 * 잡지 않기 위해. 각 repository 저장은 개별 트랜잭션으로 커밋된다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultSendMessageUsecase implements SendMessageUsecase {

    private static final int MAX_STEPS = 8;

    private final MemoryManager memoryManager;
    private final TurnRepository turnRepository;
    private final TurnEventRepository turnEventRepository;
    private final LlmClient llmClient;
    private final ChatSystemPromptProvider systemPromptProvider;
    private final TurnContextInjector turnContextInjector;
    private final ToolExecutor toolExecutor;

    @Override
    public SendMessageResult sendMessage(SendMessageCommand command) {
        Long userId = command.userId();   // 인증은 @LoginUser 리졸버가 이미 처리 (api 경계)

        // 이번 턴 LLM 입력 = 최근 대화 히스토리 + 이번 입력 + 실시간 컨텍스트
        List<LlmMessage> messages = assembleContext(userId, command.message());

        // 대화 시작을 먼저 기록 — Turn(PROCESSING) + 유저 입력 백업
        Turn turn = openTurnWithUserMessage(userId, command.message());

        // 툴 루프로 최종 응답 생성 (실패 시 Turn=FAILED)
        String answer = runLoop(turn, userId, messages);

        // 최종 응답 기록 + 대화 완료
        return completeTurn(turn, answer);
    }

    /** LLM 입력 조립: 메모리(이전 대화) + 이번 입력 + 턴 컨텍스트 주입. */
    private List<LlmMessage> assembleContext(Long userId, String currentMessage) {
        List<LlmMessage> messages = new ArrayList<>(memoryManager.recentTurns(userId));  // 메모리
        messages.add(LlmMessage.user(currentMessage));
        messages.add(LlmMessage.user(turnContextInjector.buildTurnContext(Instant.now())));  // 매 턴 주입
        return messages;
    }

    /** 대화 시작 기록: Turn을 PROCESSING으로 열고 유저 입력을 첫 이벤트로 저장한다. */
    private Turn openTurnWithUserMessage(Long userId, String message) {
        Turn turn = turnRepository.save(Turn.start(userId));
        turnEventRepository.append(TurnEvent.userMessage(turn.getTurnId(), message));
        return turn;
    }

    /**
     * 툴 루프. 매 반복 모델에 물어보고, FINAL이면 답을 반환, TOOL_CALL이면 툴 실행 후 재질의.
     * 종료: ①FINAL / ②에러(failTurn) / ③MAX_STEPS 초과.
     */
    private String runLoop(Turn turn, Long userId, List<LlmMessage> messages) {
        String systemPrompt = systemPromptProvider.build();   // 조립은 전적으로 provider가 (툴 목록 포함)

        int steps = 0;
        while (true) {
            if (++steps > MAX_STEPS) {
                // TODO : 이 부분 처리 필요 . max turn 보다 깊은 경우. ?
                throw failTurn(turn, "loop overrun (> " + MAX_STEPS + " steps)");
            }

            LlmAction action = callModel(turn, systemPrompt, messages);
            if (action.isFinal()) {
                return action.content();                                   // ① 종료
            }

            applyToolCall(turn, userId, messages, action.tool());
        }
    }

    /** 모델 1회 호출(구조화 응답). 호출 실패 시 Turn=FAILED로 남기고 예외. */
    private LlmAction callModel(Turn turn, String systemPrompt, List<LlmMessage> messages) {
        try {
            return llmClient.call(systemPrompt, messages);
        } catch (RuntimeException e) {
            log.error("LLM 호출 실패: turnId={}", turn.getTurnId(), e);
            throw failTurn(turn, "llm call failed: " + e.getMessage());
        }
    }

    /**
     * 툴 호출 요청을 실행하고 결과를 이벤트로 저장 + messages에 마커 블록으로 append.
     * tool_call_id는 서버가 발급(프롬프트 기반 — 모델이 안 줌) — TOOL_CALL/TOOL_RESULT 짝 추적용.
     * 기록은 turnMemo, 모델 되먹임은 response — 분리는 툴이 정한다.
     */
    private void applyToolCall(Turn turn, Long userId, List<LlmMessage> messages, ToolInvocation call) {
        String callId = UUID.randomUUID().toString();
        turnEventRepository.append(
                TurnEvent.toolCall(turn.getTurnId(), call.name(), callId, String.valueOf(call.arguments())));

        ToolResponse response = toolExecutor.execute(
                call, new ToolContext(userId, turn.getTurnId(), messages));

        turnEventRepository.append(
                TurnEvent.toolResult(turn.getTurnId(), callId, response.turnMemo()));

        messages.add(LlmMessage.tool(renderToolBlock(call, response)));    // 판단 근거로 남김
    }

    /** 툴 교환을 messages에 남길 표현 — 마커로 논의 대상만 감싼다. 실패는 ERROR 표시로 자가수정 유도. */
    private String renderToolBlock(ToolInvocation call, ToolResponse response) {
        String body = response.success() ? response.response() : "ERROR: " + response.response();
        return "## tool start\n"
                + call.name() + "(" + call.arguments() + ")\n"
                + body + "\n"
                + "## tool end";
    }

    /** 최종 응답을 저장하고 Turn을 COMPLETED로 마감한 뒤 결과를 만든다. */
    private SendMessageResult completeTurn(Turn turn, String answer) {
        TurnEvent assistant = turnEventRepository.append(
                TurnEvent.assistantMessage(turn.getTurnId(), answer));
        turnRepository.save(turn.complete());
        log.info("sendMessage 완료: turnId={}, userId={}", turn.getTurnId(), turn.getUserId());
        return new SendMessageResult(turn.getTurnId(), answer, assistant.getCreatedAt());
    }

    /** Turn을 FAILED로 저장하고, 던질 예외를 돌려준다. (호출부: {@code throw failTurn(...)}) */
    private LlmExecutionException failTurn(Turn turn, String reason) {
        turnRepository.save(turn.fail(reason));
        return new LlmExecutionException(reason + ": turnId=" + turn.getTurnId());
    }
}
