package com.chatbot.bravo.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 일정 REST API 시나리오 — HTTP 경계부터 usecase·soft delete까지 프로덕션 조립 그대로 관통.
 * 챗과 무관한 경로라 LlmClient 모킹은 사용하지 않는다.
 */
class ScheduleApiScenarioTest extends ChatScenarioTestBase {

    private final ObjectMapper json = new ObjectMapper();

    private MvcResult postSchedule(String bearer, String title, String scheduledAt) throws Exception {
        return mockMvc.perform(post("/schedules")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\",\"scheduleType\":\"WORK\",\"scheduledAt\":\"%s\"}"
                                .formatted(title, scheduledAt)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private long scheduleIdOf(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).get("scheduleId").asLong();
    }

    @Test
    @DisplayName("[관통] POST 등록 → GET 역순 조회 → PATCH 교체(새 id, 기존 soft delete) → DELETE")
    void should_crudSchedules_when_authenticated() throws Exception {
        String bearer = login("user1");

        // 등록 2건 — KST 7-30 15:00(06:00Z), 17:00(08:00Z)
        long first = scheduleIdOf(postSchedule(bearer, "강남 미팅", "2026-07-30T06:00:00Z"));
        long second = scheduleIdOf(postSchedule(bearer, "돌돌이 미팅", "2026-07-30T08:00:00Z"));

        // 역순(최신순) 조회 — 17:00이 먼저
        authedGet(bearer, "/schedules?from=2026-07-30&to=2026-07-30")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules[0].scheduleId").value(second))
                .andExpect(jsonPath("$.schedules[0].title").value("돌돌이 미팅"))
                .andExpect(jsonPath("$.schedules[1].scheduleId").value(first));

        // 변경 — 교체 방식: 새 scheduleId 반환, 기존 row는 soft delete
        MvcResult patched = mockMvc.perform(patch("/schedules/" + first)
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-07-30T09:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("강남 미팅"))   // 미지정 필드 유지
                .andReturn();
        long replaced = scheduleIdOf(patched);
        assertThat(replaced).isNotEqualTo(first);

        Boolean oldDeleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM schedules WHERE id = ?", Boolean.class, first);
        assertThat(oldDeleted).isTrue();
        Long apiTurnId = jdbcTemplate.queryForObject(
                "SELECT turn_id FROM schedules WHERE id = ?", Long.class, replaced);
        assertThat(apiTurnId).isNull();   // API 발 생성/변경은 turn 없음

        // 삭제 — soft delete
        mockMvc.perform(delete("/schedules/" + replaced).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        authedGet(bearer, "/schedules?from=2026-07-30&to=2026-07-30")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules.length()").value(1));   // second만 남음
    }

    @Test
    @DisplayName("[소유권] 타 유저 일정 PATCH/DELETE는 404 — 존재를 숨긴다")
    void should_hideExistence_when_notOwner() throws Exception {
        String user1 = login("user1");
        long owned = scheduleIdOf(postSchedule(user1, "user1의 일정", "2026-07-30T06:00:00Z"));

        String user2 = login("user2");
        mockMvc.perform(patch("/schedules/" + owned)
                        .header(HttpHeaders.AUTHORIZATION, user2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"탈취 시도\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/schedules/" + owned).header(HttpHeaders.AUTHORIZATION, user2))
                .andExpect(status().isNotFound());

        // user1 일정은 그대로
        Boolean deleted = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM schedules WHERE id = ?", Boolean.class, owned);
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("[검증] title 없는 등록은 400, 미인증은 401")
    void should_rejectInvalidRequests() throws Exception {
        String bearer = login("user1");
        mockMvc.perform(post("/schedules")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-07-30T06:00:00Z\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"미인증\",\"scheduledAt\":\"2026-07-30T06:00:00Z\"}"))
                .andExpect(status().isUnauthorized());
    }
}
