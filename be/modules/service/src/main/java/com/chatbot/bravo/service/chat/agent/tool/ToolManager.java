package com.chatbot.bravo.service.chat.agent.tool;

import com.chatbot.bravo.service.chat.agent.tool.schedule.ScheduleToolHandler;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * 툴 레지스트리 — 각 툴을 명시적으로 주입받아 등록한다.
 * 새 툴 추가 = 생성자 파라미터 + register 한 줄 (자동 스캔 수집 안 씀 — 등록이 코드에 보이게).
 * 라우팅(find)과 시스템 프롬프트 노출 목록(renderToolSection)을 함께 관리한다.
 */
@Component
public class ToolManager {

    private final Map<String, ToolHandler> tools;

    public ToolManager(ScheduleToolHandler scheduleToolHandler) {
        this.tools = register(
                scheduleToolHandler
        );
    }

    private static Map<String, ToolHandler> register(ToolHandler... handlers) {
        Map<String, ToolHandler> map = new LinkedHashMap<>();
        for (ToolHandler handler : handlers) {
            map.put(handler.name(), handler);
        }
        return Collections.unmodifiableMap(map);
    }

    /** 이름으로 핸들러 조회. */
    public Optional<ToolHandler> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /** 등록된 툴 이름 — 시스템 프롬프트 빌더의 조건부 섹션(tool_guidance) 판단용. */
    public SortedSet<String> toolNames() {
        return new TreeSet<>(tools.keySet());
    }

    /** 시스템 프롬프트 툴 섹션 — 각 툴의 promptText를 이어붙인다. 툴 없으면 빈 문자열. */
    public String renderToolSection() {
        if (tools.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("# 사용 가능한 도구\n");
        for (ToolHandler handler : tools.values()) {
            sb.append("\n- ").append(handler.name()).append(": ").append(handler.promptText());
        }
        return sb.toString();
    }
}
