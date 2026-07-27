package com.chatbot.bravo.service.chat;

import com.chatbot.bravo.infrastructure.chat.repository.TurnTranscriptQueryRepository;
import com.chatbot.bravo.model.chat.TranscriptEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * [디버그 전용] 유저의 전체 대화 이벤트를 시간순 평문 한 덩어리로 만든다.
 * 상태 무관(PROCESSING/FAILED 포함), TOOL_* 이벤트 포함 — LLM 컨텍스트 주입 검증용.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetTranscriptUsecase {

    private final TurnTranscriptQueryRepository turnTranscriptQueryRepository;

    public String getTranscript(Long userId) {
        List<TranscriptEvent> events = turnTranscriptQueryRepository.findAllEvents(userId);
        if (events.isEmpty()) {
            return "(no turns)";
        }

        StringBuilder sb = new StringBuilder();
        Long currentTurnId = null;
        for (TranscriptEvent e : events) {
            if (!e.getTurnId().equals(currentTurnId)) {
                currentTurnId = e.getTurnId();
                sb.append("=== turn ").append(e.getTurnId())
                        .append(" [").append(e.getTurnStatus()).append("] ===\n");
            }
            sb.append(e.getCreatedAt()).append(" ").append(e.getEventType());
            if (e.getToolName() != null) {
                sb.append("(").append(e.getToolName()).append(")");
            }
            sb.append(": ").append(e.getContent()).append("\n");
        }
        return sb.toString();
    }
}
