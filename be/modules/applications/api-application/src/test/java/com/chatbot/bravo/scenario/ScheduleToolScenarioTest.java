package com.chatbot.bravo.scenario;

import com.chatbot.bravo.infrastructure.llm.ToolParamExtractor;
import com.chatbot.bravo.model.llm.LlmAction;
import com.chatbot.bravo.model.llm.ToolInvocation;
import com.chatbot.bravo.service.chat.agent.tool.schedule.ScheduleParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * schedule 툴 루프 시나리오 — HTTP 경계부터 툴 실행·DB 반영까지 프로덕션 조립 그대로 관통.
 * LlmClient(메인 루프)와 ToolParamExtractor(툴 내부 추출)만 모킹 — 외부 OpenAI 비결정성 제거.
 */
class ScheduleToolScenarioTest extends ChatScenarioTestBase {

    @MockBean
    private ToolParamExtractor paramExtractor;

    private static final ToolInvocation SCHEDULE_CALL = new ToolInvocation("schedule", Map.of());

    @Test
    @DisplayName("[등록 성공] TOOL_CALL(schedule) → add 실행 → schedules 행 + 이벤트 4종 + FINAL 응답")
    void should_addScheduleAndComplete_when_toolLoopRuns() throws Exception {
        // given — 메인 LLM: 1차 TOOL_CALL, 2차 FINAL / 추출기: add 파라미터
        when(llmClient.call(anyString(), anyList()))
                .thenReturn(LlmAction.toolCall(SCHEDULE_CALL))
                .thenReturn(LlmAction.finalAnswer("내일 15시 회의로 등록해뒀어요!"));
        when(paramExtractor.extract(anyString(), eq(ScheduleParams.class), anyList()))
                .thenReturn(new ScheduleParams("add", null, "회의", null, "WORK",
                        "2026-07-28T15:00", null, null));

        // when
        String bearer = login("user1");
        sendMessage(bearer, "내일 3시 회의 잡아줘")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("내일 15시 회의로 등록해뒀어요!"));

        // then — 시스템 프롬프트에 툴 목록이 실제로 렌더링됐다 (카탈로그 관통 검증)
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient, times(2)).call(promptCaptor.capture(), anyList());
        assertThat(promptCaptor.getValue()).contains("- schedule:");

        // then — schedules 행: user1 소유, KST 15:00 == UTC 06:00, turn_id는 이번 턴
        Long turnId = jdbcTemplate.queryForObject(
                "SELECT id FROM turns WHERE user_id = ? AND status = 'COMPLETED'", Long.class, user1Id);
        Map<String, Object> schedule = jdbcTemplate.queryForMap(
                "SELECT * FROM schedules WHERE user_id = ?", user1Id);
        assertThat(schedule.get("title")).isEqualTo("회의");
        assertThat(schedule.get("schedule_type")).isEqualTo("WORK");
        assertThat(((Number) schedule.get("turn_id")).longValue()).isEqualTo(turnId);
        assertThat(schedule.get("scheduled_at").toString()).startsWith("2026-07-28 06:00");

        // then — 이벤트 4종 순서 + TOOL_CALL/TOOL_RESULT의 tool_call_id 짝
        var events = jdbcTemplate.queryForList(
                "SELECT type, content, tool_name, tool_call_id FROM turn_events WHERE turn_id = ? ORDER BY id", turnId);
        assertThat(events).extracting(e -> e.get("type"))
                .containsExactly("USER_MESSAGE", "TOOL_CALL", "TOOL_RESULT", "ASSISTANT_MESSAGE");
        assertThat(events.get(1).get("tool_name")).isEqualTo("schedule");
        assertThat(events.get(1).get("tool_call_id")).isNotNull()
                .isEqualTo(events.get(2).get("tool_call_id"));
        assertThat((String) events.get(2).get("content")).startsWith("schedule.add:");   // turnMemo
    }

    @Test
    @DisplayName("[정보 부족] 추출기 missing → 툴 fail 되먹임 → 모델이 되묻는 FINAL, 일정은 안 생긴다")
    void should_askBackWithoutSaving_when_paramsMissing() throws Exception {
        when(llmClient.call(anyString(), anyList()))
                .thenReturn(LlmAction.toolCall(SCHEDULE_CALL))
                .thenReturn(LlmAction.finalAnswer("몇 시로 잡을까요?"));
        when(paramExtractor.extract(anyString(), eq(ScheduleParams.class), anyList()))
                .thenReturn(new ScheduleParams("missing", "몇 시로 잡을까요?",
                        null, null, null, null, null, null));

        String bearer = login("user1");
        sendMessage(bearer, "회의 잡아줘")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("몇 시로 잡을까요?"));

        // 일정은 생기지 않고, 실패 기록(turnMemo)이 남으며, 턴은 정상 COMPLETED
        assertThat(count("SELECT COUNT(*) FROM schedules WHERE user_id = ?", user1Id)).isZero();
        Long turnId = jdbcTemplate.queryForObject(
                "SELECT id FROM turns WHERE user_id = ? AND status = 'COMPLETED'", Long.class, user1Id);
        String memo = jdbcTemplate.queryForObject(
                "SELECT content FROM turn_events WHERE turn_id = ? AND type = 'TOOL_RESULT'", String.class, turnId);
        assertThat(memo).startsWith("FAILED:").contains("몇 시로 잡을까요?");
    }
}
