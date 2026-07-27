package com.chatbot.bravo.jdbc.chat.repository;

import com.chatbot.bravo.infrastructure.chat.repository.RecentTurnQueryRepository;
import com.chatbot.bravo.infrastructure.chat.repository.TurnEventRepository;
import com.chatbot.bravo.infrastructure.chat.repository.TurnRepository;
import com.chatbot.bravo.model.chat.RecentTurn;
import com.chatbot.bravo.model.chat.Turn;
import com.chatbot.bravo.model.chat.TurnEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@ComponentScan(basePackages = "com.chatbot.bravo.jdbc.chat.repository")
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class RecentTurnQueryJdbcRepositoryTest {

    @Autowired
    private RecentTurnQueryRepository recentTurnQueryRepository;  // 테스트 대상

    @Autowired
    private TurnRepository turnRepository;         // 시드용 (raw SQL 금지 규칙)
    @Autowired
    private TurnEventRepository turnEventRepository;

    /** 완료 Turn = USER_MESSAGE + ASSISTANT_MESSAGE. 순차 저장이라 created_at은 호출 순서대로 증가. */
    private void completedTurn(Long userId, String userMsg, String asstMsg) {
        Turn turn = turnRepository.save(Turn.start(userId));
        turnEventRepository.append(TurnEvent.userMessage(turn.getTurnId(), userMsg));
        turnEventRepository.append(TurnEvent.assistantMessage(turn.getTurnId(), asstMsg));
        turnRepository.save(turn.complete());
    }

    @Test
    @DisplayName("[성공] 완료된 Turn의 USER + 최종 ASSISTANT 메시지를 반환한다")
    void should_returnUserAndAssistantMessage_when_completedTurn() {
        completedTurn(1L, "안녕", "안녕하세요");

        List<RecentTurn> result = recentTurnQueryRepository.findRecentCompletedTurns(1L, 20);

        assertThat(result).hasSize(1);
        RecentTurn t = result.get(0);
        assertThat(t.getTurnId()).isNotNull().isPositive();
        assertThat(t.getUserMessage()).isEqualTo("안녕");
        assertThat(t.getAssistantMessage()).isEqualTo("안녕하세요");
        assertThat(t.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("[스펙/경계] 중간 TOOL_CALL/TOOL_RESULT는 제외되고 USER + 최종 ASSISTANT만 나온다")
    void should_excludeToolEvents_when_findRecent() {
        Turn turn = turnRepository.save(Turn.start(1L));
        Long id = turn.getTurnId();
        turnEventRepository.append(TurnEvent.userMessage(id, "오늘 서울 날씨"));
        turnEventRepository.append(TurnEvent.toolCall(id, "get_weather", "call_1", "{\"city\":\"서울\"}"));
        turnEventRepository.append(TurnEvent.toolResult(id, "call_1", "{\"temp\":25}"));
        turnEventRepository.append(TurnEvent.assistantMessage(id, "오늘 서울은 맑고 25도입니다."));
        turnRepository.save(turn.complete());

        List<RecentTurn> result = recentTurnQueryRepository.findRecentCompletedTurns(1L, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserMessage()).isEqualTo("오늘 서울 날씨");
        assertThat(result.get(0).getAssistantMessage()).isEqualTo("오늘 서울은 맑고 25도입니다.");
    }

    @Test
    @DisplayName("[경계] PROCESSING/미완료 Turn은 조회에서 제외된다 (COMPLETED만)")
    void should_excludeNonCompletedTurns_when_findRecent() {
        completedTurn(1L, "완료된 대화", "답변");
        // PROCESSING (complete 안 함)
        Turn processing = turnRepository.save(Turn.start(1L));
        turnEventRepository.append(TurnEvent.userMessage(processing.getTurnId(), "처리 중"));

        List<RecentTurn> result = recentTurnQueryRepository.findRecentCompletedTurns(1L, 20);

        assertThat(result).extracting(RecentTurn::getUserMessage).containsExactly("완료된 대화");
    }

    @Test
    @DisplayName("[경계] 결과는 오래된→최신 순으로 정렬된다")
    void should_returnOldestToNewest_when_findRecent() {
        completedTurn(1L, "첫번째", "a1");
        completedTurn(1L, "두번째", "a2");
        completedTurn(1L, "세번째", "a3");

        List<RecentTurn> result = recentTurnQueryRepository.findRecentCompletedTurns(1L, 20);

        assertThat(result).extracting(RecentTurn::getUserMessage)
                .containsExactly("첫번째", "두번째", "세번째");
    }

    @Test
    @DisplayName("[경계] size로 최신 N개만 조회하되, 반환은 오래된→최신")
    void should_limitToRecentN_when_sizeGiven() {
        completedTurn(1L, "t1", "a1");
        completedTurn(1L, "t2", "a2");
        completedTurn(1L, "t3", "a3");

        List<RecentTurn> result = recentTurnQueryRepository.findRecentCompletedTurns(1L, 2);

        // 최신 2개(t2, t3)를 오래된→최신으로
        assertThat(result).extracting(RecentTurn::getUserMessage).containsExactly("t2", "t3");
    }

    @Test
    @DisplayName("[경계] 20턴 초과 시 최신 20턴만, 오래된→최신 순서로 반환한다")
    void should_returnLatest20InOrder_when_moreThan20Turns() {
        for (int i = 1; i <= 25; i++) {
            completedTurn(1L, "q" + i, "a" + i);
        }

        List<RecentTurn> result = recentTurnQueryRepository.findRecentCompletedTurns(1L, 20);

        // 최신 20턴(q6~q25)만, 오래된→최신
        assertThat(result).extracting(RecentTurn::getUserMessage)
                .containsExactly(java.util.stream.IntStream.rangeClosed(6, 25)
                        .mapToObj(i -> "q" + i).toArray(String[]::new));
        assertThat(result).extracting(RecentTurn::getAssistantMessage)
                .containsExactly(java.util.stream.IntStream.rangeClosed(6, 25)
                        .mapToObj(i -> "a" + i).toArray(String[]::new));
    }

    @Test
    @DisplayName("[경계] 다른 유저의 대화는 섞이지 않는다")
    void should_isolateByUser_when_findRecent() {
        completedTurn(1L, "user1 대화", "a");
        completedTurn(2L, "user2 대화", "b");

        assertThat(recentTurnQueryRepository.findRecentCompletedTurns(1L, 20))
                .extracting(RecentTurn::getUserMessage).containsExactly("user1 대화");
    }

    @Test
    @DisplayName("[실패] 완료 대화가 없으면 빈 리스트를 반환한다")
    void should_returnEmpty_when_noCompletedTurns() {
        assertThat(recentTurnQueryRepository.findRecentCompletedTurns(999L, 20)).isEmpty();
    }
}
