package com.chatbot.bravo.api.schedule.dto;

import com.chatbot.bravo.model.schedule.Schedule;

import java.util.List;

public record SchedulesResponse(
        List<ScheduleResponse> schedules
) {
    public static SchedulesResponse from(List<Schedule> schedules) {
        return new SchedulesResponse(schedules.stream().map(ScheduleResponse::from).toList());
    }
}
