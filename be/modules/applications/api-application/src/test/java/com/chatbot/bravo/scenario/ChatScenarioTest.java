package com.chatbot.bravo.scenario;

import com.chatbot.bravo.model.llm.LlmAction;
import com.chatbot.bravo.model.llm.LlmMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 챗봇 주요 시나리오 — 로그인 → 대화 → 조회를 실제 조립(LLM만 mock)으로 관통한다.
 */
class ChatScenarioTest extends ChatScenarioTestBase {

    /** LLM stub — 모든 호출에 대해 주어진 최종 답변을 반환한다. */
    private void stubFinalAnswer(String answer) {
        when(llmClient.call(anyString(), anyList())).thenReturn(LlmAction.finalAnswer(answer));
    }

    /** LLM에 실제로 전달된 messages를 캡처한다. */
    private List<LlmMessage> capturePrompt() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LlmMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(llmClient).call(anyString(), captor.capture());
        return captor.getValue();
    }

    // ---------------------------------------------------------------- 1. 부팅/시드

    @Test
    @DisplayName("[부팅] 조립 루트가 뜨고 시드가 성립한다 — user 3명, user1 0턴, user2 20턴")
    void should_bootAssemblyAndSeed() throws Exception {
        assertThat(count("SELECT COUNT(*) FROM users")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM turns WHERE user_id = ?", user1Id)).isEqualTo(0);
        assertThat(count("SELECT COUNT(*) FROM turns WHERE user_id = ?", user2Id)).isEqualTo(20);
        // 20턴 × 2이벤트 = 40
        assertThat(count("SELECT COUNT(*) FROM turn_events e JOIN turns t ON t.id = e.turn_id WHERE t.user_id = ?",
                user2Id)).isEqualTo(40);

        assertThat(login("user1")).startsWith("Bearer ");
    }

    // ---------------------------------------------------------------- 2. 빈 대화 유저 첫 대화

    @Test
    @DisplayName("[빈 대화] user1 로그인 → 첫 메시지 전송 → 응답 저장 + recent 1턴, LLM엔 히스토리 없이 전달")
    void should_startFirstConversation_when_emptyUser() throws Exception {
        stubFinalAnswer("안녕하세요! 무엇을 도와드릴까요?");
        String bearer = login("user1");

        // 전송 → 200 + 응답 매핑
        sendMessage(bearer, "안녕")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnId").isNumber())
                .andExpect(jsonPath("$.message").value("안녕하세요! 무엇을 도와드릴까요?"));

        // recent → 방금 만든 1턴
        authedGet(bearer, "/chat/turns/recent")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turns.length()").value(1))
                .andExpect(jsonPath("$.turns[0].userMessage").value("안녕"))
                .andExpect(jsonPath("$.turns[0].assistantMessage").value("안녕하세요! 무엇을 도와드릴까요?"));

        // 실제 저장: 완료 턴 1 + 이벤트 2(USER/ASSISTANT)
        assertThat(count("SELECT COUNT(*) FROM turns WHERE user_id = ? AND status = 'COMPLETED'", user1Id)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM turn_events e JOIN turns t ON t.id = e.turn_id WHERE t.user_id = ?",
                user1Id)).isEqualTo(2);

        // LLM 프롬프트: 이전 대화 없음 → [USER(현재), USER(ctx)] 2개
        List<LlmMessage> prompt = capturePrompt();
        assertThat(prompt).hasSize(2);
        assertThat(prompt.get(0).content()).isEqualTo("안녕");
        assertThat(prompt.get(1).content()).contains("system-context");   // 턴 컨텍스트
    }

    // ---------------------------------------------------------------- 3. 기존 컨텍스트 유저 대화

    @Test
    @DisplayName("[기존 컨텍스트] user2 recent 20턴 조회 + 새 메시지의 LLM 프롬프트에 이전 20턴(40메시지)이 앞에 실린다")
    void should_carryContext_when_userHasHistory() throws Exception {
        stubFinalAnswer("이어서 답변드릴게요.");
        String bearer = login("user2");

        // recent 20턴 (오래된→최신)
        authedGet(bearer, "/chat/turns/recent?size=20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turns.length()").value(20))
                .andExpect(jsonPath("$.turns[0].userMessage").value("질문 1"))
                .andExpect(jsonPath("$.turns[19].userMessage").value("질문 20"));

        // 새 메시지 전송
        sendMessage(bearer, "다음 질문이야").andExpect(status().isOk());

        // LLM 프롬프트: [ (질문1,답변1) ... (질문20,답변20) , 현재입력, ctx ] = 40 + 2 = 42
        List<LlmMessage> prompt = capturePrompt();
        assertThat(prompt).hasSize(42);
        assertThat(prompt.get(0).content()).isEqualTo("질문 1");
        assertThat(prompt.get(1).content()).isEqualTo("답변 1");
        assertThat(prompt.get(38).content()).isEqualTo("질문 20");
        assertThat(prompt.get(39).content()).isEqualTo("답변 20");
        assertThat(prompt.get(40).content()).isEqualTo("다음 질문이야");
        assertThat(prompt.get(41).content()).contains("system-context");
    }

    // ---------------------------------------------------------------- 4. 인증

    @Test
    @DisplayName("[인증] Authorization 헤더 없이 전송하면 401")
    void should_return401_when_noAuthHeader() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/chat/turns")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"안녕\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[인증] 잘못된 비밀번호 로그인은 401")
    void should_return401_when_wrongPassword() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- 5. size 검증 (MVC 경로)

    @Test
    @DisplayName("[검증] recent size=0 → 400, size=21 → 400 (@RequestParam + usecase 검증 관통)")
    void should_return400_when_sizeOutOfRange() throws Exception {
        String bearer = login("user1");

        authedGet(bearer, "/chat/turns/recent?size=0").andExpect(status().isBadRequest());
        authedGet(bearer, "/chat/turns/recent?size=21").andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- 6. 격리

    @Test
    @DisplayName("[격리] user1의 recent에는 user2의 대화가 섞이지 않는다")
    void should_isolateConversationsByUser() throws Exception {
        String bearer = login("user1");

        authedGet(bearer, "/chat/turns/recent")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turns.length()").value(0));
    }
}
