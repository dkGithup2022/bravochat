package com.chatbot.bravo.service.chat.agent.memory;

import com.chatbot.bravo.infrastructure.chat.repository.RecentTurnQueryRepository;
import com.chatbot.bravo.infrastructure.chat.repository.TurnEventRepository;
import com.chatbot.bravo.model.chat.RecentTurn;
import com.chatbot.bravo.model.chat.TurnEvent;
import com.chatbot.bravo.model.chat.TurnEventType;
import com.chatbot.bravo.model.llm.LlmMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 기본 메모리 매니저. 최근 완료 대화 N턴을 발생 순서대로
 * [USER, (도구기록)*, ASSISTANT] 메시지로 평탄화한다.
 *
 * <p>도구기록: 각 턴의 TOOL_RESULT 이벤트(turnMemo)의 memo를 "[도구기록] ..." TOOL
 * 메시지로 끼워 넣는다. 어시스턴트 발언("등록하겠습니다")만 남고 실제 실행 증거가
 * 사라지면, 다음 턴의 모델이 결과를 지어내거나(했다고 단정) 얼어붙는(호출 없이 재확인만
 * 반복) 문제가 생긴다 — 실기록 기반 수정.
 */
@Component
@RequiredArgsConstructor
class DefaultMemoryManager implements MemoryManager {

    private static final int RECENT_TURNS = 20;

    private final RecentTurnQueryRepository recentTurnQueryRepository;
    private final TurnEventRepository turnEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<LlmMessage> recentTurns(Long userId) {
        List<RecentTurn> history = recentTurnQueryRepository.findRecentCompletedTurns(userId, RECENT_TURNS);

        List<LlmMessage> messages = new ArrayList<>();
        for (RecentTurn t : history) {                 // 오래된→최신
            messages.add(LlmMessage.user(t.getUserMessage()));
            appendToolRecords(messages, t.getTurnId());
            messages.add(LlmMessage.assistant(t.getAssistantMessage()));
        }
        return messages;
    }

    /** 턴 안에서 실행된 도구 결과를 발생 순서(id 오름차순)대로 끼워 넣는다. */
    private void appendToolRecords(List<LlmMessage> messages, Long turnId) {
        for (TurnEvent e : turnEventRepository.findAllByTurnIdInOrder(turnId)) {
            if (e.getType() == TurnEventType.TOOL_RESULT) {
                messages.add(LlmMessage.tool("[도구기록] " + memoOf(e.getContent())));
            }
        }
    }

    /** TOOL_RESULT content(turnMemo JSON)에서 memo를 뽑는다. 형식이 다르면 원문 그대로. */
    private String memoOf(String content) {
        try {
            JsonNode memo = objectMapper.readTree(content).get("memo");
            if (memo != null && memo.isTextual() && !memo.asText().isBlank()) {
                return memo.asText();
            }
        } catch (JsonProcessingException ignored) {
            // memo 래핑이 아닌 툴의 기록 — 원문을 그대로 노출
        }
        return content;
    }
}
