package com.chatbot.bravo.service.chat.agent.memory;

import com.chatbot.bravo.infrastructure.chat.repository.RecentTurnQueryRepository;
import com.chatbot.bravo.model.chat.RecentTurn;
import com.chatbot.bravo.model.llm.LlmMessage;
import com.chatbot.bravo.model.llm.LlmRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DefaultMemoryManager 단위 테스트 — 최근 완료 대화(RecentTurn)를
 * [USER, ASSISTANT] 메시지로 평탄화하는 매핑 로직 검증.
 * (RecentTurn 조회 자체는 repository-jdbc 테스트에서 커버)
 */
@ExtendWith(MockitoExtension.class)
class DefaultMemoryManagerTest {

    @Mock
    private RecentTurnQueryRepository recentTurnQueryRepository;

    @InjectMocks
    private DefaultMemoryManager memoryManager;

    private RecentTurn turn(String userMsg, String asstMsg) {
        return new RecentTurn(1L, userMsg, asstMsg, Instant.now());
    }

    @Test
    @DisplayName("완료 대화 2턴을 [USER, ASSISTANT] × 2 = 4개 메시지로 순서 보존하며 평탄화한다")
    void should_flattenToUserAssistantPairs_when_historyExists() {
        // given — 오래된→최신 순서로 2턴
        when(recentTurnQueryRepository.findRecentCompletedTurns(7L, 20))
                .thenReturn(List.of(turn("첫 질문", "첫 답변"), turn("둘째 질문", "둘째 답변")));

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
