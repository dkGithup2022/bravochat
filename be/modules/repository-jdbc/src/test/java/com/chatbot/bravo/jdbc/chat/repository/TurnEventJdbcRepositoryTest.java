package com.chatbot.bravo.jdbc.chat.repository;

import com.chatbot.bravo.infrastructure.chat.repository.TurnEventRepository;
import com.chatbot.bravo.model.chat.TurnEvent;
import com.chatbot.bravo.model.chat.TurnEventType;
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
class TurnEventJdbcRepositoryTest {

    private static final long TURN_ID = 1L;   // FK 미사용이라 실제 turns 행 불필요

    @Autowired
    private TurnEventRepository eventRepository;             // 테스트 대상 (인터페이스)

    @Autowired
    private TurnEventEntityRepository eventEntityRepository; // soft-delete 픽스처용

    @Test
    @DisplayName("[성공] append한 이벤트를 turnId로 조회한다 — 전 필드 온전 + auditing")
    void should_returnAppendedEvent_when_foundByTurnId() {
        eventRepository.append(TurnEvent.userMessage(TURN_ID, "안녕하세요"));

        List<TurnEvent> events = eventRepository.findAllByTurnIdInOrder(TURN_ID);

        assertThat(events).hasSize(1);
        TurnEvent e = events.get(0);
        assertThat(e.getEventId()).isNotNull().isPositive();
        assertThat(e.getTurnId()).isEqualTo(TURN_ID);
        assertThat(e.getType()).isEqualTo(TurnEventType.USER_MESSAGE);
        assertThat(e.getContent()).isEqualTo("안녕하세요");
        assertThat(e.getToolName()).isNull();
        assertThat(e.getToolCallId()).isNull();
        assertThat(e.getCreatedAt()).isNotNull();
        assertThat(e.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("[스펙] TOOL_CALL/TOOL_RESULT는 toolName/toolCallId 필드가 저장된다")
    void should_persistToolFields_when_toolEventsAppended() {
        eventRepository.append(TurnEvent.toolCall(TURN_ID, "get_weather", "call_1", "{\"city\":\"서울\"}"));
        eventRepository.append(TurnEvent.toolResult(TURN_ID, "call_1", "{\"temp\":25}"));

        List<TurnEvent> events = eventRepository.findAllByTurnIdInOrder(TURN_ID);

        TurnEvent call = events.get(0);
        assertThat(call.getType()).isEqualTo(TurnEventType.TOOL_CALL);
        assertThat(call.getToolName()).isEqualTo("get_weather");
        assertThat(call.getToolCallId()).isEqualTo("call_1");

        TurnEvent result = events.get(1);
        assertThat(result.getType()).isEqualTo(TurnEventType.TOOL_RESULT);
        assertThat(result.getToolName()).isNull();
        assertThat(result.getToolCallId()).isEqualTo("call_1");
    }

    @Test
    @DisplayName("[경계] findAllByTurnIdInOrder는 append 순서(=id 오름차순)로 반환한다")
    void should_returnInInsertionOrder_when_foundByTurnId() {
        eventRepository.append(TurnEvent.userMessage(TURN_ID, "질문"));
        eventRepository.append(TurnEvent.toolCall(TURN_ID, "tool", "c1", "{}"));
        eventRepository.append(TurnEvent.toolResult(TURN_ID, "c1", "{}"));
        eventRepository.append(TurnEvent.assistantMessage(TURN_ID, "답변"));

        List<TurnEvent> events = eventRepository.findAllByTurnIdInOrder(TURN_ID);

        assertThat(events).extracting(TurnEvent::getType).containsExactly(
                TurnEventType.USER_MESSAGE, TurnEventType.TOOL_CALL,
                TurnEventType.TOOL_RESULT, TurnEventType.ASSISTANT_MESSAGE);
    }

    @Test
    @DisplayName("[성공] appendAll은 여러 이벤트를 한 번에 저장한다")
    void should_persistAll_when_appendAll() {
        eventRepository.appendAll(List.of(
                TurnEvent.userMessage(TURN_ID, "q"),
                TurnEvent.assistantMessage(TURN_ID, "a")));

        assertThat(eventRepository.findAllByTurnIdInOrder(TURN_ID)).hasSize(2);
    }

    @Test
    @DisplayName("[경계] 다른 turnId의 이벤트는 섞이지 않는다")
    void should_isolateByTurnId_when_foundByTurnId() {
        eventRepository.append(TurnEvent.userMessage(1L, "turn1"));
        eventRepository.append(TurnEvent.userMessage(2L, "turn2"));

        assertThat(eventRepository.findAllByTurnIdInOrder(1L))
                .extracting(TurnEvent::getContent).containsExactly("turn1");
        assertThat(eventRepository.findAllByTurnIdInOrder(2L))
                .extracting(TurnEvent::getContent).containsExactly("turn2");
    }

    @Test
    @DisplayName("[실패/경계] soft-delete된 이벤트는 조회에서 제외된다")
    void should_excludeEvent_when_softDeleted() {
        TurnEvent saved = eventRepository.append(TurnEvent.userMessage(TURN_ID, "지울 것"));
        TurnEventEntity entity = eventEntityRepository
                .findByTurnIdAndIsDeletedFalseOrderByIdAsc(TURN_ID).get(0);
        eventEntityRepository.save(entity.softDelete());

        assertThat(eventRepository.findAllByTurnIdInOrder(TURN_ID)).isEmpty();
    }
}
