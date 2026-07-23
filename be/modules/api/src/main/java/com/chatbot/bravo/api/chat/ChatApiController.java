package com.chatbot.bravo.api.chat;

import com.chatbot.bravo.api.chat.dto.RecentTurnsResponse;
import com.chatbot.bravo.api.chat.dto.SendMessageRequest;
import com.chatbot.bravo.api.chat.dto.SendMessageResponse;
import com.chatbot.bravo.exception.auth.InvalidSessionException;
import com.chatbot.bravo.service.chat.GetRecentTurnsUsecase;
import com.chatbot.bravo.service.chat.SendMessageUsecase;
import com.chatbot.bravo.service.chat.dto.GetRecentTurnsQuery;
import com.chatbot.bravo.service.chat.dto.GetRecentTurnsResult;
import com.chatbot.bravo.service.chat.dto.SendMessageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "대화 API")
public class ChatApiController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GetRecentTurnsUsecase getRecentTurnsUsecase;
    private final SendMessageUsecase sendMessageUsecase;

    @Operation(summary = "대화 요청 — 메시지를 전송하고 최종 응답을 반환. "
            + "Authorization 헤더의 세션으로 userId를 해석하고 최근 20턴을 컨텍스트로 사용한다.")
    @PostMapping("/chat/turns")
    public SendMessageResponse sendMessage(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody SendMessageRequest request) {
        String sessionKey = extractSessionKey(authorization);
        log.info("POST /chat/turns");
        SendMessageResult result = sendMessageUsecase.sendMessage(request.toCommand(sessionKey));
        return SendMessageResponse.from(result);
    }

    private String extractSessionKey(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new InvalidSessionException("missing or malformed Authorization header");
        }
        String sessionKey = authorization.substring(BEARER_PREFIX.length()).trim();
        if (sessionKey.isEmpty()) {
            throw new InvalidSessionException("empty session key");
        }
        return sessionKey;
    }

    @Operation(summary = "최근 대화 조회 — 완료된 Turn의 사용자 입력 + 최종 응답만 (오래된→최신). "
            + "userId는 인증 계층 도입 전까지 임시 쿼리파라미터로 수신.")
    @GetMapping("/chat/turns/recent")
    public RecentTurnsResponse getRecentTurns(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /chat/turns/recent - userId={}, size={}", userId, size);
        GetRecentTurnsResult result =
                getRecentTurnsUsecase.getRecentTurns(new GetRecentTurnsQuery(userId, size));
        return RecentTurnsResponse.from(result);
    }
}
