package com.chatbot.bravo.api.chat;

import com.chatbot.bravo.api.chat.dto.RecentTurnsResponse;
import com.chatbot.bravo.api.chat.dto.SendMessageRequest;
import com.chatbot.bravo.api.chat.dto.SendMessageResponse;
import com.chatbot.bravo.model.auth.LoginSession;
import com.chatbot.bravo.model.chat.RecentTurn;
import com.chatbot.bravo.service.chat.GetRecentTurnsUsecase;
import com.chatbot.bravo.service.chat.SendMessageUsecase;
import com.chatbot.bravo.service.chat.dto.GetRecentTurnsQuery;
import com.chatbot.bravo.service.chat.dto.GetRecentTurnsResult;
import com.chatbot.bravo.service.chat.dto.SendMessageCommand;
import com.chatbot.bravo.service.chat.dto.SendMessageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChatApiController 단위 테스트 — MockMvc 없이 메서드 직접 호출.
 * 인증 세션의 userId가 usecase 입력(command/query)에 실리는 배선과 응답 DTO 매핑을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ChatApiControllerTest {

    @Mock
    private GetRecentTurnsUsecase getRecentTurnsUsecase;

    @Mock
    private SendMessageUsecase sendMessageUsecase;

    @InjectMocks
    private ChatApiController controller;

    private LoginSession session(Long userId) {
        Instant now = Instant.now();
        return new LoginSession(1L, "sk", userId, now, now, now, now);
    }

    @Test
    @DisplayName("sendMessage: 세션 userId + 요청 message로 command를 만들어 위임하고 결과를 응답으로 매핑한다")
    void should_wireUserIdAndMapResponse_when_sendMessage() {
        Instant createdAt = Instant.now();
        when(sendMessageUsecase.sendMessage(any(SendMessageCommand.class)))
                .thenReturn(new SendMessageResult(100L, "응답입니다", createdAt));

        SendMessageResponse response =
                controller.sendMessage(session(7L), new SendMessageRequest("안녕"));

        // 응답 매핑
        assertThat(response.turnId()).isEqualTo(100L);
        assertThat(response.message()).isEqualTo("응답입니다");
        assertThat(response.createdAt()).isEqualTo(createdAt);

        // 배선: userId는 세션에서, message는 요청에서
        ArgumentCaptor<SendMessageCommand> captor = ArgumentCaptor.forClass(SendMessageCommand.class);
        verify(sendMessageUsecase).sendMessage(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(7L);
        assertThat(captor.getValue().message()).isEqualTo("안녕");
    }

    @Test
    @DisplayName("getRecentTurns: 세션 userId + size로 query를 만들어 위임하고 목록을 응답으로 매핑한다")
    void should_wireUserIdAndSize_when_getRecentTurns() {
        RecentTurn turn = new RecentTurn(1L, "질문", "답변", Instant.now());
        when(getRecentTurnsUsecase.getRecentTurns(any(GetRecentTurnsQuery.class)))
                .thenReturn(new GetRecentTurnsResult(List.of(turn)));

        RecentTurnsResponse response = controller.getRecentTurns(session(7L), 5);

        // 응답 매핑
        assertThat(response.turns()).hasSize(1);
        assertThat(response.turns().get(0).userMessage()).isEqualTo("질문");
        assertThat(response.turns().get(0).assistantMessage()).isEqualTo("답변");

        // 배선: userId + size
        ArgumentCaptor<GetRecentTurnsQuery> captor = ArgumentCaptor.forClass(GetRecentTurnsQuery.class);
        verify(getRecentTurnsUsecase).getRecentTurns(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(7L);
        assertThat(captor.getValue().size()).isEqualTo(5);
    }
}
