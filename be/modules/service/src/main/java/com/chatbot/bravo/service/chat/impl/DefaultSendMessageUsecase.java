package com.chatbot.bravo.service.chat.impl;

import com.chatbot.bravo.exception.chat.LlmExecutionException;
import com.chatbot.bravo.infrastructure.chat.repository.RecentTurnQueryRepository;
import com.chatbot.bravo.infrastructure.chat.repository.TurnEventRepository;
import com.chatbot.bravo.infrastructure.chat.repository.TurnRepository;
import com.chatbot.bravo.infrastructure.llm.LlmClient;
import com.chatbot.bravo.model.chat.RecentTurn;
import com.chatbot.bravo.model.chat.Turn;
import com.chatbot.bravo.model.chat.TurnEvent;
import com.chatbot.bravo.model.llm.LlmMessage;
import com.chatbot.bravo.model.llm.LlmResponse;
import com.chatbot.bravo.service.auth.SessionManager;
import com.chatbot.bravo.service.chat.SendMessageUsecase;
import com.chatbot.bravo.service.chat.dto.SendMessageCommand;
import com.chatbot.bravo.service.chat.dto.SendMessageResult;
import com.chatbot.bravo.service.chat.orchestrator.TurnContextInjector;
import com.chatbot.bravo.service.chat.orchestrator.systemprompt.ChatSystemPromptProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * SendMessage 오케스트레이션 (MVP — 도구 없음, 순수 대화).
 *
 * 흐름: sessionKey→userId → 최근 20턴 히스토리 로드 → 메시지 조립(+턴 주입) →
 *       Turn(PROCESSING) + USER_MESSAGE 저장 → LLM 호출 → ASSISTANT_MESSAGE 저장 →
 *       Turn 완료 → 최종 응답 반환.
 *
 * 주의: 메서드에 @Transactional을 걸지 않는다 — LLM 호출(느린 외부 I/O) 동안 DB 커넥션을
 *       잡지 않기 위해. 각 repository 저장은 개별 트랜잭션으로 커밋된다.
 * Phase 2: 도구가 생기면 LLM 호출~이벤트 append 부분을 while(hasToolUse) 루프로 확장.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultSendMessageUsecase implements SendMessageUsecase {

    private static final int HISTORY_TURNS = 20;

    private final SessionManager sessionManager;
    private final RecentTurnQueryRepository recentTurnQueryRepository;
    private final TurnRepository turnRepository;
    private final TurnEventRepository turnEventRepository;
    private final LlmClient llmClient;
    private final ChatSystemPromptProvider systemPromptProvider;
    private final TurnContextInjector turnContextInjector;

    @Override
    public SendMessageResult sendMessage(SendMessageCommand command) {
        // 1. 최상단: 세션 검증 → userId
        Long userId = sessionManager.check(command.sessionKey()).getUserId();

        // 2. 컨텍스트 조립: 최근 20턴 히스토리 + 현재 입력 + 턴 주입
        List<LlmMessage> messages = loadContext(userId, command.message());

        // 3. Turn 시작 + USER_MESSAGE 저장
        Turn turn = turnRepository.save(Turn.start(userId));
        Long turnId = turn.getTurnId();
        turnEventRepository.append(TurnEvent.userMessage(turnId, 1, command.message()));

        // 4. LLM 호출 (외부 I/O — 트랜잭션 밖)
        LlmResponse response;
        try {
            response = llmClient.call(systemPromptProvider.systemPrompt(), messages);
        } catch (RuntimeException e) {
            log.error("LLM 호출 실패: turnId={}", turnId, e);
            turnRepository.save(turn.fail("llm call failed: " + e.getMessage()));
            throw new LlmExecutionException("llm call failed: turnId=" + turnId);
        }

        // MVP: 도구 미지원 → 첫 응답이 최종. (Phase 2: hasToolUse면 툴 실행 후 재호출)
        if (response.hasToolUse()) {
            turnRepository.save(turn.fail("tool use not supported in MVP"));
            throw new LlmExecutionException("tool use not supported in MVP: turnId=" + turnId);
        }

        // 5. ASSISTANT_MESSAGE 저장 + Turn 완료
        TurnEvent assistant =
                turnEventRepository.append(TurnEvent.assistantMessage(turnId, 2, response.assistantText()));
        turnRepository.save(turn.complete());
        log.info("sendMessage 완료: turnId={}, userId={}", turnId, userId);

        return new SendMessageResult(turnId, response.assistantText(), assistant.getCreatedAt());
    }

    /** 최근 완료 대화 20턴을 [USER, ASSISTANT] 메시지로 평탄화 + 현재 입력 + 턴 주입. */
    private List<LlmMessage> loadContext(Long userId, String currentMessage) {
        List<RecentTurn> history = recentTurnQueryRepository.findRecentCompletedTurns(userId, HISTORY_TURNS);
        List<LlmMessage> messages = new ArrayList<>();
        for (RecentTurn t : history) {                 // 오래된→최신
            messages.add(LlmMessage.user(t.getUserMessage()));
            messages.add(LlmMessage.assistant(t.getAssistantMessage()));
        }
        messages.add(LlmMessage.user(currentMessage));
        messages.add(LlmMessage.user(turnContextInjector.buildTurnContext(Instant.now())));  // 매 턴 주입
        return messages;
    }
}
