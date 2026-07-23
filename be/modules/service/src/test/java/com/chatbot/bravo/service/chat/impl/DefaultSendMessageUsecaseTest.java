package com.chatbot.bravo.service.chat.impl;

import com.chatbot.bravo.exception.chat.LlmExecutionException;
import com.chatbot.bravo.infrastructure.chat.repository.RecentTurnQueryRepository;
import com.chatbot.bravo.infrastructure.chat.repository.TurnEventRepository;
import com.chatbot.bravo.infrastructure.chat.repository.TurnRepository;
import com.chatbot.bravo.infrastructure.llm.LlmClient;
import com.chatbot.bravo.model.auth.LoginSession;
import com.chatbot.bravo.model.chat.RecentTurn;
import com.chatbot.bravo.model.chat.Turn;
import com.chatbot.bravo.model.chat.TurnEvent;
import com.chatbot.bravo.model.chat.TurnEventType;
import com.chatbot.bravo.model.chat.TurnStatus;
import com.chatbot.bravo.model.llm.LlmMessage;
import com.chatbot.bravo.model.llm.LlmResponse;
import com.chatbot.bravo.model.llm.LlmRole;
import com.chatbot.bravo.service.auth.SessionManager;
import com.chatbot.bravo.service.chat.dto.SendMessageCommand;
import com.chatbot.bravo.service.chat.dto.SendMessageResult;
import com.chatbot.bravo.service.chat.orchestrator.TurnContextInjector;
import com.chatbot.bravo.service.chat.orchestrator.systemprompt.ChatSystemPromptProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSendMessageUsecaseTest {

    private final SessionManager sessionManager = mock(SessionManager.class);
    private final RecentTurnQueryRepository recentTurnQueryRepository = mock(RecentTurnQueryRepository.class);
    private final TurnRepository turnRepository = mock(TurnRepository.class);
    private final TurnEventRepository turnEventRepository = mock(TurnEventRepository.class);
    private final LlmClient llmClient = mock(LlmClient.class);
    private final ChatSystemPromptProvider systemPromptProvider = mock(ChatSystemPromptProvider.class);
    private final TurnContextInjector turnContextInjector = mock(TurnContextInjector.class);

    private final DefaultSendMessageUsecase usecase = new DefaultSendMessageUsecase(
            sessionManager, recentTurnQueryRepository, turnRepository, turnEventRepository,
            llmClient, systemPromptProvider, turnContextInjector);

    private Turn processingTurn() {
        Instant now = Instant.now();
        return new Turn(100L, 7L, TurnStatus.PROCESSING, null, null, now, now);
    }

    @Test
    void 정상흐름_세션검증_히스토리_저장_완료() {
        when(sessionManager.check("skey")).thenReturn(LoginSession.issue(7L));
        when(recentTurnQueryRepository.findRecentCompletedTurns(7L, 20))
                .thenReturn(List.of(new RecentTurn(1L, "이전 질문", "이전 답변", Instant.now())));
        when(turnRepository.save(any(Turn.class))).thenReturn(processingTurn());
        when(turnEventRepository.append(any(TurnEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(systemPromptProvider.systemPrompt()).thenReturn("SYSTEM");
        when(turnContextInjector.buildTurnContext(any())).thenReturn("<ctx>");
        when(llmClient.call(anyString(), anyList())).thenReturn(LlmResponse.finalText("최종 응답"));

        SendMessageResult result = usecase.sendMessage(new SendMessageCommand("skey", "안녕"));

        assertThat(result.turnId()).isEqualTo(100L);
        assertThat(result.message()).isEqualTo("최종 응답");
        assertThat(result.createdAt()).isNotNull();

        // 세션→userId, 히스토리 20턴 조회
        verify(sessionManager).check("skey");
        verify(recentTurnQueryRepository).findRecentCompletedTurns(7L, 20);

        // LLM 호출: systemPrompt + [USER(hist), ASSISTANT(hist), USER(현재), USER(ctx)]
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LlmMessage>> msgCaptor = ArgumentCaptor.forClass(List.class);
        verify(llmClient).call(eq("SYSTEM"), msgCaptor.capture());
        List<LlmMessage> msgs = msgCaptor.getValue();
        assertThat(msgs).hasSize(4);
        assertThat(msgs.get(0)).isEqualTo(new LlmMessage(LlmRole.USER, "이전 질문"));
        assertThat(msgs.get(1)).isEqualTo(new LlmMessage(LlmRole.ASSISTANT, "이전 답변"));
        assertThat(msgs.get(2)).isEqualTo(new LlmMessage(LlmRole.USER, "안녕"));
        assertThat(msgs.get(3)).isEqualTo(new LlmMessage(LlmRole.USER, "<ctx>"));

        // 이벤트 저장: USER_MESSAGE(seq1) → ASSISTANT_MESSAGE(seq2)
        ArgumentCaptor<TurnEvent> eventCaptor = ArgumentCaptor.forClass(TurnEvent.class);
        verify(turnEventRepository, org.mockito.Mockito.times(2)).append(eventCaptor.capture());
        List<TurnEvent> events = eventCaptor.getAllValues();
        assertThat(events.get(0).getType()).isEqualTo(TurnEventType.USER_MESSAGE);
        assertThat(events.get(0).getSequence()).isEqualTo(1);
        assertThat(events.get(0).getContent()).isEqualTo("안녕");
        assertThat(events.get(1).getType()).isEqualTo(TurnEventType.ASSISTANT_MESSAGE);
        assertThat(events.get(1).getSequence()).isEqualTo(2);
        assertThat(events.get(1).getContent()).isEqualTo("최종 응답");

        // Turn: start + complete
        ArgumentCaptor<Turn> turnCaptor = ArgumentCaptor.forClass(Turn.class);
        verify(turnRepository, org.mockito.Mockito.times(2)).save(turnCaptor.capture());
        assertThat(turnCaptor.getAllValues().get(1).getStatus()).isEqualTo(TurnStatus.COMPLETED);
    }

    @Test
    void LLM_실패시_Turn_FAILED_저장_후_예외() {
        when(sessionManager.check("skey")).thenReturn(LoginSession.issue(7L));
        when(recentTurnQueryRepository.findRecentCompletedTurns(anyLong(), anyInt())).thenReturn(List.of());
        when(turnRepository.save(any(Turn.class))).thenReturn(processingTurn());
        when(turnEventRepository.append(any(TurnEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(systemPromptProvider.systemPrompt()).thenReturn("SYSTEM");
        when(turnContextInjector.buildTurnContext(any())).thenReturn("<ctx>");
        when(llmClient.call(anyString(), anyList())).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> usecase.sendMessage(new SendMessageCommand("skey", "안녕")))
                .isInstanceOf(LlmExecutionException.class);

        ArgumentCaptor<Turn> turnCaptor = ArgumentCaptor.forClass(Turn.class);
        verify(turnRepository, org.mockito.Mockito.times(2)).save(turnCaptor.capture());
        assertThat(turnCaptor.getAllValues().get(1).getStatus()).isEqualTo(TurnStatus.FAILED);
    }

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }
}
