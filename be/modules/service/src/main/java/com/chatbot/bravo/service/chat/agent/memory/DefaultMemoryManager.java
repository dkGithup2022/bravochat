package com.chatbot.bravo.service.chat.agent.memory;

import com.chatbot.bravo.infrastructure.chat.repository.RecentTurnQueryRepository;
import com.chatbot.bravo.model.chat.RecentTurn;
import com.chatbot.bravo.model.llm.LlmMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 기본 메모리 매니저. 최근 완료 대화 N턴을 조회해 [USER, ASSISTANT] 메시지로 평탄화한다.
 * (TOOL_* 내부 이벤트는 RecentTurn 조회 단계에서 이미 제외됨)
 */
@Component
@RequiredArgsConstructor
class DefaultMemoryManager implements MemoryManager {

    private static final int RECENT_TURNS = 20;

    private final RecentTurnQueryRepository recentTurnQueryRepository;

    @Override
    public List<LlmMessage> recentTurns(Long userId) {
        List<RecentTurn> history = recentTurnQueryRepository.findRecentCompletedTurns(userId, RECENT_TURNS);

        List<LlmMessage> messages = new ArrayList<>();
        for (RecentTurn t : history) {                 // 오래된→최신
            messages.add(LlmMessage.user(t.getUserMessage()));
            messages.add(LlmMessage.assistant(t.getAssistantMessage()));
        }
        return messages;
    }
}
