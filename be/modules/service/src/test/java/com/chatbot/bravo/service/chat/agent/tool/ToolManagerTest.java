package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.infrastructure.llm.ToolParamExtractor;
import com.chatbot.bravo.infrastructure.schedule.repository.ScheduleRepository;
import com.chatbot.bravo.service.chat.agent.tool.schedule.ScheduleToolHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/** ToolManager — 명시적 등록 레지스트리. 실제 등록 구성(ScheduleToolHandler) 그대로 검증한다. */
@ExtendWith(MockitoExtension.class)
class ToolManagerTest {

    @Mock private ToolParamExtractor paramExtractor;
    @Mock private ScheduleRepository scheduleRepository;

    private ToolManager manager() {
        return new ToolManager(new ScheduleToolHandler(paramExtractor, scheduleRepository));
    }

    @Test
    @DisplayName("[성공] 등록된 툴을 이름으로 찾는다 / 미등록은 empty")
    void should_findByName_when_registered() {
        ToolManager manager = manager();

        assertThat(manager.find("schedule")).isPresent();
        assertThat(manager.find("unknown")).isEmpty();
        assertThat(manager.toolNames()).containsExactly("schedule");
    }

    @Test
    @DisplayName("[성공] 툴 섹션은 각 툴의 name + promptText로 렌더링된다")
    void should_renderNameAndPromptText_when_renderToolSection() {
        String section = manager().renderToolSection();

        assertThat(section)
                .contains("# 사용 가능한 도구")
                .contains("- schedule: ")
                .contains("일정");
    }
}
