package com.chatbot.bravo.service.chat.agent;

import com.chatbot.bravo.exception.chat.LlmExecutionException;
import com.chatbot.bravo.infrastructure.chat.repository.TurnEventRepository;
import com.chatbot.bravo.infrastructure.chat.repository.TurnRepository;
import com.chatbot.bravo.infrastructure.llm.LlmClient;
import com.chatbot.bravo.model.chat.Turn;
import com.chatbot.bravo.model.chat.TurnEvent;
import com.chatbot.bravo.model.chat.TurnEventType;
import com.chatbot.bravo.model.chat.TurnStatus;
import com.chatbot.bravo.model.llm.LlmAction;
import com.chatbot.bravo.model.llm.LlmMessage;
import com.chatbot.bravo.model.llm.LlmRole;
import com.chatbot.bravo.model.llm.ToolInvocation;
import com.chatbot.bravo.service.chat.agent.memory.MemoryManager;
import com.chatbot.bravo.service.chat.agent.systemprompt.ChatSystemPromptProvider;
import com.chatbot.bravo.service.chat.agent.tool.ToolExecutor;
import com.chatbot.bravo.service.chat.agent.tool.ToolResponse;
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
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DefaultSendMessageUsecase(툴 루프) 단위 테스트.
 *
 * <p>하위 호출(LlmClient/ToolExecutor/repository)을 전부 모킹하고,
 * 분기별로 <b>① LLM에 실제로 어떤 프롬프트(messages)를 보내는지</b>와
 * <b>② 실제로 어떤 TurnEvent가 저장되는지</b>를 검증한다.
 * 보내는 프롬프트는 매 테스트에서 {@link #logPrompt}로 콘솔에 출력한다.
 *
 * 매트릭스: {툴 없음 / 툴 있음 / 에러} × {이전 대화 있음 / 없음}
 */
@ExtendWith(MockitoExtension.class)
class DefaultSendMessageUsecaseTest {

    @Mock private MemoryManager memoryManager;
    @Mock private TurnRepository turnRepository;
    @Mock private TurnEventRepository turnEventRepository;
    @Mock private LlmClient llmClient;
    @Mock private ChatSystemPromptProvider systemPromptProvider;
    @Mock private TurnContextInjector turnContextInjector;
    @Mock private ToolExecutor toolExecutor;

    @InjectMocks
    private DefaultSendMessageUsecase usecase;

    private static final Long USER_ID = 7L;

    /** 저장/프롬프트 조립 등 분기와 무관한 공통 스텁. */
    private void commonStubs() {
        when(turnRepository.save(any(Turn.class))).thenReturn(processingTurn());
        when(turnEventRepository.append(any(TurnEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(systemPromptProvider.build()).thenReturn("SYSTEM_PROMPT");
        when(turnContextInjector.buildTurnContext(any())).thenReturn("<ctx>");
    }

    private Turn processingTurn() {
        Instant now = Instant.now();
        return new Turn(100L, USER_ID, TurnStatus.PROCESSING, null, null, now, now);
    }

    /** LLM에 보낸 프롬프트를 콘솔에 그대로 출력 — "각 분기가 어떤 프롬프트를 보내는지" 눈으로 확인용. */
    private void logPrompt(String label, String systemPrompt, List<LlmMessage> messages) {
        System.out.println("\n===== PROMPT [" + label + "] =====");
        System.out.println("[SYSTEM] " + systemPrompt);
        for (LlmMessage m : messages) {
            System.out.println("[" + m.role() + "] " + m.content());
        }
        System.out.println("===== END [" + label + "] =====");
    }

    // ---------------------------------------------------------------------
    // 툴 없음 (FINAL 1바퀴) × 이전 대화 없음 / 있음
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("[툴X·이전대화 없음] 프롬프트는 [USER(현재), USER(ctx)] 2개만 보내고 정상 저장/완료한다")
    void should_sendCurrentAndContextOnly_when_noHistoryAndFinal() {
        // given
        commonStubs();
        when(memoryManager.recentTurns(USER_ID)).thenReturn(List.of());   // 이전 대화 없음
        when(llmClient.call(anyString(), anyList())).thenReturn(LlmAction.finalAnswer("반가워요"));

        // when
        SendMessageResult result = usecase.sendMessage(new SendMessageCommand(USER_ID, "안녕"));

        // then — 보낸 프롬프트
        List<LlmMessage> sent = captureSingleCallMessages("툴X·이전대화 없음");
        assertThat(sent).containsExactly(
                new LlmMessage(LlmRole.USER, "안녕"),
                new LlmMessage(LlmRole.USER, "<ctx>"));

        // then — 실제 저장된 턴: 유저 입력 → 최종 응답, Turn은 PROCESSING→COMPLETED
        List<TurnEvent> events = captureAllEvents();
        assertThat(events).hasSize(2);
        assertEvent(events.get(0), TurnEventType.USER_MESSAGE, "안녕");
        assertEvent(events.get(1), TurnEventType.ASSISTANT_MESSAGE, "반가워요");
        assertThat(events).allSatisfy(e -> assertThat(e.getTurnId()).isEqualTo(100L));   // 같은 턴에 귀속
        assertTurnLifecycle(TurnStatus.COMPLETED);
        assertThat(result.message()).isEqualTo("반가워요");
    }

    @Test
    @DisplayName("[툴X·이전대화 있음] 이전 대화를 앞에 붙여 [hist U/A, USER(현재), USER(ctx)] 4개를 보낸다")
    void should_prependHistory_when_hasHistoryAndFinal() {
        // given
        commonStubs();
        when(memoryManager.recentTurns(USER_ID))
                .thenReturn(List.of(LlmMessage.user("이전 질문"), LlmMessage.assistant("이전 답변")));
        when(llmClient.call(anyString(), anyList())).thenReturn(LlmAction.finalAnswer("최종 응답"));

        // when
        usecase.sendMessage(new SendMessageCommand(USER_ID, "안녕"));

        // then — 이전 대화가 프롬프트 앞에 온다
        List<LlmMessage> sent = captureSingleCallMessages("툴X·이전대화 있음");
        assertThat(sent).containsExactly(
                new LlmMessage(LlmRole.USER, "이전 질문"),
                new LlmMessage(LlmRole.ASSISTANT, "이전 답변"),
                new LlmMessage(LlmRole.USER, "안녕"),
                new LlmMessage(LlmRole.USER, "<ctx>"));

        // then — 저장은 이전 대화와 무관하게 이번 턴의 입력/응답만: USER_MESSAGE → ASSISTANT_MESSAGE
        List<TurnEvent> events = captureAllEvents();
        assertThat(events).hasSize(2);
        assertEvent(events.get(0), TurnEventType.USER_MESSAGE, "안녕");
        assertEvent(events.get(1), TurnEventType.ASSISTANT_MESSAGE, "최종 응답");
        assertTurnLifecycle(TurnStatus.COMPLETED);
    }

    // ---------------------------------------------------------------------
    // 툴 있음 (TOOL_CALL → 실행 → 재질의 → FINAL)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("[툴 O] TOOL_CALL이면 툴 실행 결과를 messages에 붙여 재질의하고, 이벤트를 U→TC→TR→A 순서로 저장한다")
    void should_appendToolBlockAndRequery_when_toolCall() {
        // given — 1차 TOOL_CALL, 2차 FINAL.
        // messages는 루프에서 재사용되는 같은 리스트라, 호출 시점에 스냅샷을 떠서 프롬프트 변화를 관찰한다.
        commonStubs();
        when(memoryManager.recentTurns(USER_ID))
                .thenReturn(List.of(LlmMessage.user("이전 질문"), LlmMessage.assistant("이전 답변")));
        ToolInvocation call = new ToolInvocation("get_weather", Map.of("city", "서울"));
        List<List<LlmMessage>> sentPrompts = new java.util.ArrayList<>();
        when(llmClient.call(anyString(), anyList())).thenAnswer(inv -> {
            sentPrompts.add(List.copyOf(inv.getArgument(1)));   // 호출 순간의 스냅샷
            return sentPrompts.size() == 1
                    ? LlmAction.toolCall(call)
                    : LlmAction.finalAnswer("서울은 맑고 25도예요");
        });
        when(toolExecutor.execute(any(), any())).thenReturn(ToolResponse.ok("날씨: 25도"));

        // when
        usecase.sendMessage(new SendMessageCommand(USER_ID, "오늘 서울 날씨"));

        // then — LLM은 2번 호출, 툴은 1번 실행
        verify(llmClient, times(2)).call(anyString(), anyList());
        verify(toolExecutor, times(1)).execute(any(), any());

        // 1차 프롬프트: 이전대화 + 현재 + ctx (4개), 아직 툴 블록 없음
        List<LlmMessage> first = sentPrompts.get(0);
        logPrompt("툴 O · 1차 질의", "SYSTEM_PROMPT", first);
        assertThat(first).hasSize(4);

        // 2차 프롬프트: 마지막에 TOOL 블록이 붙어 재질의 (5개)
        List<LlmMessage> second = sentPrompts.get(1);
        logPrompt("툴 O · 2차 재질의", "SYSTEM_PROMPT", second);
        assertThat(second).hasSize(5);
        LlmMessage toolMsg = second.get(4);
        assertThat(toolMsg.role()).isEqualTo(LlmRole.TOOL);
        assertThat(toolMsg.content())
                .contains("## tool start")
                .contains("get_weather")
                .contains("날씨: 25도")
                .contains("## tool end");

        // then — 실제 저장된 턴: 유저입력 → 툴호출 → 툴결과 → 최종응답 (4건, 순서대로)
        List<TurnEvent> events = captureAllEvents();
        assertThat(events).hasSize(4);
        assertEvent(events.get(0), TurnEventType.USER_MESSAGE, "오늘 서울 날씨");
        assertThat(events.get(1).getType()).isEqualTo(TurnEventType.TOOL_CALL);
        assertThat(events.get(1).getToolName()).isEqualTo("get_weather");
        assertThat(events.get(1).getContent()).isEqualTo("{city=서울}");        // 저장된 툴 인자
        assertEvent(events.get(2), TurnEventType.TOOL_RESULT, "날씨: 25도");    // 툴이 남긴 기록(turnMemo)
        // tool_call_id는 서버 발급 — TOOL_CALL/TOOL_RESULT가 같은 id로 짝지어진다
        assertThat(events.get(1).getToolCallId()).isNotBlank();
        assertThat(events.get(2).getToolCallId()).isEqualTo(events.get(1).getToolCallId());
        assertEvent(events.get(3), TurnEventType.ASSISTANT_MESSAGE, "서울은 맑고 25도예요");
        assertThat(events).allSatisfy(e -> assertThat(e.getTurnId()).isEqualTo(100L));
        assertTurnLifecycle(TurnStatus.COMPLETED);
    }

    // ---------------------------------------------------------------------
    // 에러 (LLM 호출 실패 / MAX_STEPS 초과)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("[에러·이전대화 없음] LLM 호출이 실패하면 Turn을 FAILED로 저장하고 LlmExecutionException을 던진다")
    void should_failTurn_when_llmThrows() {
        // given
        commonStubs();
        when(memoryManager.recentTurns(USER_ID)).thenReturn(List.of());
        when(llmClient.call(anyString(), anyList())).thenThrow(new RuntimeException("boom"));

        // when / then
        assertThatThrownBy(() -> usecase.sendMessage(new SendMessageCommand(USER_ID, "안녕")))
                .isInstanceOf(LlmExecutionException.class);

        // then — 유저 입력만 저장되고 최종 응답은 없음, Turn은 PROCESSING→FAILED
        List<TurnEvent> events = captureAllEvents();
        assertThat(events).hasSize(1);
        assertEvent(events.get(0), TurnEventType.USER_MESSAGE, "안녕");
        assertTurnLifecycle(TurnStatus.FAILED);
    }

    @Test
    @DisplayName("[가드] 모델이 계속 TOOL_CALL만 하면 MAX_STEPS(8회) 후 FAILED로 종료한다 (무한루프 방지)")
    void should_failTurn_when_exceedsMaxSteps() {
        // given — 항상 TOOL_CALL (FINAL을 절대 안 줌)
        commonStubs();
        when(memoryManager.recentTurns(USER_ID)).thenReturn(List.of());
        when(llmClient.call(anyString(), anyList()))
                .thenReturn(LlmAction.toolCall(new ToolInvocation("loop_tool", Map.of())));
        when(toolExecutor.execute(any(), any())).thenReturn(ToolResponse.ok("again"));

        // when / then
        assertThatThrownBy(() -> usecase.sendMessage(new SendMessageCommand(USER_ID, "안녕")))
                .isInstanceOf(LlmExecutionException.class);

        // then — 8스텝 동안만 호출/실행
        verify(llmClient, times(8)).call(anyString(), anyList());
        verify(toolExecutor, times(8)).execute(any(), any());

        // 저장된 턴: USER 1 + (TOOL_CALL+TOOL_RESULT)×8 = 17건, 최종 응답 없음, Turn FAILED
        List<TurnEvent> events = captureAllEvents();
        assertThat(events).hasSize(17);
        assertEvent(events.get(0), TurnEventType.USER_MESSAGE, "안녕");
        assertThat(events).filteredOn(e -> e.getType() == TurnEventType.TOOL_CALL).hasSize(8);
        assertThat(events).filteredOn(e -> e.getType() == TurnEventType.TOOL_RESULT).hasSize(8);
        assertThat(events).extracting(TurnEvent::getType).doesNotContain(TurnEventType.ASSISTANT_MESSAGE);
        assertTurnLifecycle(TurnStatus.FAILED);
    }

    // ---------------------------------------------------------------------
    // 헬퍼
    // ---------------------------------------------------------------------

    /** 단일 LLM 호출의 messages를 캡처하고 콘솔에 출력한다. */
    private List<LlmMessage> captureSingleCallMessages(String label) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LlmMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(llmClient).call(anyString(), captor.capture());
        List<LlmMessage> messages = captor.getValue();
        logPrompt(label, "SYSTEM_PROMPT", messages);
        return messages;
    }

    /** 저장 순서대로 append된 TurnEvent 전체. */
    private List<TurnEvent> captureAllEvents() {
        ArgumentCaptor<TurnEvent> captor = ArgumentCaptor.forClass(TurnEvent.class);
        verify(turnEventRepository, org.mockito.Mockito.atLeastOnce()).append(captor.capture());
        return captor.getAllValues();
    }

    /** 한 TurnEvent의 타입+내용을 함께 검증한다. */
    private void assertEvent(TurnEvent event, TurnEventType type, String content) {
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getContent()).isEqualTo(content);
    }

    /**
     * Turn 생명주기 검증: save는 정확히 2번 — [0] 시작(PROCESSING) → [1] 최종(expected).
     * "턴이 실제로 어떻게 저장되는지"의 뼈대.
     */
    private void assertTurnLifecycle(TurnStatus expectedFinal) {
        ArgumentCaptor<Turn> captor = ArgumentCaptor.forClass(Turn.class);
        verify(turnRepository, times(2)).save(captor.capture());
        List<Turn> saved = captor.getAllValues();
        assertThat(saved.get(0).getStatus()).isEqualTo(TurnStatus.PROCESSING);   // 시작 기록
        assertThat(saved.get(0).getUserId()).isEqualTo(USER_ID);
        assertThat(saved.get(1).getStatus()).isEqualTo(expectedFinal);            // 최종 마감
    }
}
