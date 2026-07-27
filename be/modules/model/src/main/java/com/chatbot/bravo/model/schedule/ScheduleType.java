package com.chatbot.bravo.model.schedule;

/**
 * 일정 분류. 값은 LLM 툴 인자로 들어오므로 미스매치 흡수 규약을 이 enum이 소유한다.
 */
public enum ScheduleType {
    HEALTH,
    PERSONAL,
    WORK,
    ETC;

    /** 대소문자 무관 매칭. 미스매치·null은 ETC로 흡수 (LLM 인자 안전망). */
    public static ScheduleType fromOrEtc(String value) {
        if (value == null) {
            return ETC;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ETC;
        }
    }
}
