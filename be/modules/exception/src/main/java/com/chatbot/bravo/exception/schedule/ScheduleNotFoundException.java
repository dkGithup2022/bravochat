package com.chatbot.bravo.exception.schedule;

import com.chatbot.bravo.exception.EntityNotFoundException;

/**
 * 일정 없음(404). 타 유저 소유 일정도 존재를 숨기기 위해 동일하게 처리한다.
 */
public class ScheduleNotFoundException extends EntityNotFoundException {

    public ScheduleNotFoundException(Long scheduleId) {
        super("schedule not found: id=" + scheduleId, 404, "일정을 찾을 수 없습니다");
    }
}
