package com.chatbot.bravo.service.chat.agent.memory;

import com.chatbot.bravo.infrastructure.chat.repository.RecentTurnQueryRepository;
import com.chatbot.bravo.infrastructure.chat.repository.TurnEventRepository;
import com.chatbot.bravo.model.chat.RecentTurn;
import com.chatbot.bravo.model.chat.TurnEvent;
import com.chatbot.bravo.model.llm.LlmMessage;
import com.chatbot.bravo.model.llm.LlmRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DefaultMemoryManager 단위 테스트 — 최근 완료 대화(RecentTurn)를
 * [USER, (도구기록)*, ASSISTANT] 메시지로 평탄화하는 매핑 로직 검증.
 * (RecentTurn/이벤트 조회 자체는 repository-jdbc 테스트에서 커버)
 */
@ExtendWith(MockitoExtension.class)
class DefaultMemoryManagerTest {

    @Mock
    private RecentTurnQueryRepository recentTurnQueryRepository;

    @Mock
    private TurnEventRepository turnEventRepository;

    private DefaultMemoryManager memoryManager;

    @BeforeEach
    void setUp() {
        memoryManager = new DefaultMemoryManager(
                recentTurnQueryRepository, turnEventRepository, new ObjectMapper());
    }

    private RecentTurn turn(Long turnId, String userMsg, String asstMsg) {
        return new RecentTurn(turnId, userMsg, asstMsg, Instant.now());
    }

    @Test
    @DisplayName("도구 없는 대화 2턴은 [USER, ASSISTANT] × 2 = 4개 메시지로 순서 보존하며 평탄화한다")
    void should_flattenToUserAssistantPairs_when_historyExists() {
        // given — 오래된→최신 순서로 2턴, 도구 이벤트 없음
        when(recentTurnQueryRepository.findRecentCompletedTurns(7L, 20))
                .thenReturn(List.of(turn(1L, "첫 질문", "첫 답변"), turn(2L, "둘째 질문", "둘째 답변")));
        when(turnEventRepository.findAllByTurnIdInOrder(1L)).thenReturn(List.of());
        when(turnEventRepository.findAllByTurnIdInOrder(2L)).thenReturn(List.of());

        // when
        List<LlmMessage> messages = memoryManager.recentTurns(7L);

        // then — 턴 순서 유지 + 각 턴은 USER 다음 ASSISTANT
        assertThat(messages).containsExactly(
                new LlmMessage(LlmRole.USER, "첫 질문"),
                new LlmMessage(LlmRole.ASSISTANT, "첫 답변"),
                new LlmMessage(LlmRole.USER, "둘째 질문"),
                new LlmMessage(LlmRole.ASSISTANT, "둘째 답변"));
        verify(recentTurnQueryRepository).findRecentCompletedTurns(7L, 20);
    }

    @Test
    @DisplayName("툴 실행 턴은 TOOL_RESULT의 memo가 [도구기록] TOOL 메시지로 USER와 ASSISTANT 사이에 발생 순서대로 끼워진다")
    void should_insertToolRecordsBetweenUserAndAssistant_when_turnHasToolResults() {
        // given — 한 턴에서 도구 2회 실행 (turnMemo JSON에 memo 포함)
        when(recentTurnQueryRepository.findRecentCompletedTurns(7L, 20))
                .thenReturn(List.of(turn(1L, "일정 2개 등록해줘", "둘 다 등록했습니다")));
        when(turnEventRepository.findAllByTurnIdInOrder(1L)).thenReturn(List.of(
                TurnEvent.userMessage(1L, "일정 2개 등록해줘"),
                TurnEvent.toolCall(1L, "schedule", "c1", "{}"),
                TurnEvent.toolResult(1L, "c1", "{\"tool\":\"schedule\",\"memo\":\"schedule.add: [1] ui 수정 배포\"}"),
                TurnEvent.toolCall(1L, "schedule", "c2", "{}"),
                TurnEvent.toolResult(1L, "c2", "{\"tool\":\"schedule\",\"memo\":\"schedule.add: [2] llm 호출방식 변경\"}"),
                TurnEvent.assistantMessage(1L, "둘 다 등록했습니다")));

        // when
        List<LlmMessage> messages = memoryManager.recentTurns(7L);

        // then — USER → 도구기록(발생 순서) → ASSISTANT. TOOL_CALL/USER/ASSISTANT 이벤트는 중복 유입되지 않는다
        assertThat(messages).containsExactly(
                new LlmMessage(LlmRole.USER, "일정 2개 등록해줘"),
                new LlmMessage(LlmRole.TOOL, "[도구기록] schedule.add: [1] ui 수정 배포"),
                new LlmMessage(LlmRole.TOOL, "[도구기록] schedule.add: [2] llm 호출방식 변경"),
                new LlmMessage(LlmRole.ASSISTANT, "둘 다 등록했습니다"));
    }

    @Test
    @DisplayName("[경계] TOOL_RESULT content가 memo 래핑 JSON이 아니면 원문 그대로 [도구기록]에 싣는다")
    void should_fallbackToRawContent_when_toolResultIsNotMemoJson() {
        // given
        when(recentTurnQueryRepository.findRecentCompletedTurns(7L, 20))
                .thenReturn(List.of(turn(1L, "질문", "답변")));
        when(turnEventRepository.findAllByTurnIdInOrder(1L)).thenReturn(List.of(
                TurnEvent.toolResult(1L, "c1", "plain text 기록")));

        // when
        List<LlmMessage> messages = memoryManager.recentTurns(7L);

        // then
        assertThat(messages).containsExactly(
                new LlmMessage(LlmRole.USER, "질문"),
                new LlmMessage(LlmRole.TOOL, "[도구기록] plain text 기록"),
                new LlmMessage(LlmRole.ASSISTANT, "답변"));
    }

    @Test
    @DisplayName("완료 대화가 없으면 빈 메시지 리스트를 반환한다")
    void should_returnEmpty_when_noHistory() {
        // given
        when(recentTurnQueryRepository.findRecentCompletedTurns(7L, 20)).thenReturn(List.of());

        // when
        List<LlmMessage> messages = memoryManager.recentTurns(7L);

        // then
        assertThat(messages).isEmpty();
    }
}
