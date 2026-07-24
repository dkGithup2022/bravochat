package com.chatbot.bravo.scenario;

import com.chatbot.bravo.infrastructure.llm.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 챗봇 시나리오 테스트 베이스 — 프로덕션과 동일한 조립 루트를 그대로 부팅한다.
 *
 * <p>{@link LlmClient}만 {@link MockBean}으로 교체(외부 OpenAI 비결정성 제거)하고,
 * 컨트롤러·서비스·리포지토리·H2·@LoginUser 리졸버·예외 핸들러는 전부 실제 빈을 쓴다.
 * 요청은 MockMvc로 HTTP 경계부터 관통한다.
 *
 * <p>시드는 {@link #seed()}에서 JdbcTemplate로 프로그램적으로 넣는다:
 * user1(빈 대화) / user2(완료 20턴) / user3(존재만). 비밀번호는 모두 "password1234".
 * {@code @Transactional} 롤백으로 매 테스트가 동일 시드에서 출발한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ChatScenarioTestBase {

    /** BCrypt("password1234") — 실제 로그인 API를 통과시키기 위한 고정 해시. */
    private static final String PASSWORD_HASH = "$2a$10$3cgyvfPAXtijkNKB0hckIei7BqwvAK9RF4IVlz6w0/rR3yz3MuxPS";
    protected static final String PASSWORD = "password1234";
    protected static final int USER2_TURN_COUNT = 20;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /** OpenAI 대신 꽂히는 mock — 각 테스트에서 응답을 프로그래밍하고 프롬프트를 캡처한다. */
    @MockBean
    protected LlmClient llmClient;

    protected long user1Id;   // 빈 대화
    protected long user2Id;   // 완료 20턴
    protected long user3Id;   // 존재만 (격리 확인)

    @BeforeEach
    void seed() {
        user1Id = seedUser("user1");
        user2Id = seedUser("user2");
        user3Id = seedUser("user3");

        // user2 — 완료 20턴(오래된→최신). createdAt을 1분 간격으로 증가시켜 순서를 못박는다.
        Instant base = Instant.parse("2026-07-20T00:00:00Z");
        for (int i = 1; i <= USER2_TURN_COUNT; i++) {
            seedCompletedTurn(user2Id, base.plus(i, ChronoUnit.MINUTES), "질문 " + i, "답변 " + i);
        }
    }

    // ----------------------------------------------------------------- 시드 헬퍼

    protected long seedUser(String username) {
        Timestamp now = Timestamp.from(Instant.now());
        return insertReturningId(
                "INSERT INTO users (username, password_hash, is_deleted, created_at, updated_at) "
                        + "VALUES (?, ?, FALSE, ?, ?)",
                username, PASSWORD_HASH, now, now);
    }

    /** 완료 Turn 1개 + USER_MESSAGE/ASSISTANT_MESSAGE 이벤트 2개를 넣는다. */
    protected long seedCompletedTurn(long userId, Instant createdAt, String userMsg, String asstMsg) {
        Timestamp ts = Timestamp.from(createdAt);
        long turnId = insertReturningId(
                "INSERT INTO turns (user_id, status, completed_at, is_deleted, created_at, updated_at) "
                        + "VALUES (?, 'COMPLETED', ?, FALSE, ?, ?)",
                userId, ts, ts, ts);
        insertEvent(turnId, "USER_MESSAGE", userMsg, ts);
        insertEvent(turnId, "ASSISTANT_MESSAGE", asstMsg, ts);
        return turnId;
    }

    protected void insertEvent(long turnId, String type, String content, Timestamp ts) {
        jdbcTemplate.update(
                "INSERT INTO turn_events (turn_id, type, content, is_deleted, created_at, updated_at) "
                        + "VALUES (?, ?, ?, FALSE, ?, ?)",
                turnId, type, content, ts, ts);
    }

    private long insertReturningId(String sql, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("생성된 키가 없습니다: " + sql);
        }
        return key.longValue();
    }

    // ----------------------------------------------------------------- HTTP 헬퍼

    /** 실제 로그인 API로 세션 키 획득 → "Bearer {키}". */
    protected String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andExpect(status().isNoContent())
                .andReturn();
        return result.getResponse().getHeader(HttpHeaders.AUTHORIZATION);
    }

    protected ResultActions authedGet(String bearer, String path) throws Exception {
        return mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    protected ResultActions sendMessage(String bearer, String message) throws Exception {
        return mockMvc.perform(post("/chat/turns")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"%s\"}".formatted(message)));
    }

    protected long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? -1 : value;
    }
}
