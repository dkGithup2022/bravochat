package com.chatbot.bravo.api.chat;

import com.chatbot.bravo.api.auth.LoginUser;
import com.chatbot.bravo.api.chat.dto.RecentTurnsResponse;
import com.chatbot.bravo.api.chat.dto.SendMessageRequest;
import com.chatbot.bravo.api.chat.dto.SendMessageResponse;
import com.chatbot.bravo.model.auth.LoginSession;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "대화 API")
public class ChatApiController {

    private final GetRecentTurnsUsecase getRecentTurnsUsecase;
    private final SendMessageUsecase sendMessageUsecase;

    @Operation(summary = "대화 요청 — 메시지를 전송하고 최종 응답을 반환. 인증된 세션의 userId로 처리, 최근 20턴을 컨텍스트로 사용.")
    @PostMapping("/chat/turns")
    public SendMessageResponse sendMessage(
            @LoginUser LoginSession loginSession,
            @Valid @RequestBody SendMessageRequest request) {
        log.info("POST /chat/turns");
        SendMessageResult result = sendMessageUsecase.sendMessage(request.toCommand(loginSession.getUserId()));
        return SendMessageResponse.from(result);
    }

    @Operation(summary = "최근 대화 조회 — 완료된 Turn의 사용자 입력 + 최종 응답만 (오래된→최신). 인증된 세션의 userId로 조회.")
    @GetMapping("/chat/turns/recent")
    public RecentTurnsResponse getRecentTurns(
            @LoginUser LoginSession loginSession,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /chat/turns/recent - size={}", size);
        GetRecentTurnsResult result =
                getRecentTurnsUsecase.getRecentTurns(new GetRecentTurnsQuery(loginSession.getUserId(), size));
        return RecentTurnsResponse.from(result);
    }
}
