package com.chatbot.bravo.service.chat;

import com.chatbot.bravo.exception.chat.InvalidRecentTurnSizeException;
import com.chatbot.bravo.infrastructure.chat.repository.RecentTurnQueryRepository;
import com.chatbot.bravo.model.chat.RecentTurn;
import com.chatbot.bravo.service.chat.dto.GetRecentTurnsQuery;
import com.chatbot.bravo.service.chat.dto.GetRecentTurnsResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GetRecentTurnsUsecase 단위 테스트 — 유일한 실제 로직인 size 경계 검증(1~20)에 집중.
 * 범위 밖이면 InvalidRecentTurnSizeException(400), 범위 안이면 repo로 위임.
 */
@ExtendWith(MockitoExtension.class)
class GetRecentTurnsUsecaseTest {

    @Mock
    private RecentTurnQueryRepository recentTurnQueryRepository;

    @InjectMocks
    private GetRecentTurnsUsecase usecase;

    private RecentTurn turn(String userMsg, String asstMsg) {
        return new RecentTurn(1L, userMsg, asstMsg, Instant.now());
    }

    @Test
    @DisplayName("[경계] size=20(최대)이면 통과하고 조회 결과를 그대로 반환한다")
    void should_returnTurns_when_sizeIsMax() {
        // given
        List<RecentTurn> found = List.of(turn("q1", "a1"), turn("q2", "a2"));
        when(recentTurnQueryRepository.findRecentCompletedTurns(7L, 20)).thenReturn(found);

        // when
        GetRecentTurnsResult result = usecase.getRecentTurns(new GetRecentTurnsQuery(7L, 20));

        // then
        assertThat(result.turns()).isEqualTo(found);
        verify(recentTurnQueryRepository).findRecentCompletedTurns(7L, 20);
    }

    @Test
    @DisplayName("[경계] size=1(최소)이면 통과하여 repo로 위임한다")
    void should_returnTurns_when_sizeIsMin() {
        // given
        when(recentTurnQueryRepository.findRecentCompletedTurns(7L, 1)).thenReturn(List.of(turn("q", "a")));

        // when
        GetRecentTurnsResult result = usecase.getRecentTurns(new GetRecentTurnsQuery(7L, 1));

        // then
        assertThat(result.turns()).hasSize(1);
        verify(recentTurnQueryRepository).findRecentCompletedTurns(7L, 1);
    }

    @Test
    @DisplayName("[실패] size=0이면 InvalidRecentTurnSizeException(400) — 조회 안 함")
    void should_throwInvalidSize_when_sizeIsZero() {
        // when / then
        assertThatThrownBy(() -> usecase.getRecentTurns(new GetRecentTurnsQuery(7L, 0)))
                .isInstanceOf(InvalidRecentTurnSizeException.class)
                .satisfies(e -> assertThat(((InvalidRecentTurnSizeException) e).httpStatusCode()).isEqualTo(400));

        verify(recentTurnQueryRepository, never()).findRecentCompletedTurns(anyLong(), anyInt());
    }

    @Test
    @DisplayName("[실패] size=21이면(최대 초과) InvalidRecentTurnSizeException(400) — 조회 안 함")
    void should_throwInvalidSize_when_sizeExceedsMax() {
        // when / then
        assertThatThrownBy(() -> usecase.getRecentTurns(new GetRecentTurnsQuery(7L, 21)))
                .isInstanceOf(InvalidRecentTurnSizeException.class);

        verify(recentTurnQueryRepository, never()).findRecentCompletedTurns(anyLong(), anyInt());
    }
}
