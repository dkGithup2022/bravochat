package com.chatbot.bravo.api.chat;

import com.chatbot.bravo.api.chat.dto.RecentTurnsResponse;
import com.chatbot.bravo.api.chat.dto.SendMessageRequest;
import com.chatbot.bravo.api.chat.dto.SendMessageResponse;
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

    @Operation(summary = "대화 요청 — 사용자 메시지를 전송하고 최종 응답을 반환. "
            + "userId는 인증 계층 도입 전까지 임시 쿼리파라미터. (오케스트레이션 미구현 — 현재 501)")
    @PostMapping("/chat/turns")
    public SendMessageResponse sendMessage(
            @RequestParam Long userId,
            @Valid @RequestBody SendMessageRequest request) {
        log.info("POST /chat/turns - userId={}", userId);
        SendMessageResult result = sendMessageUsecase.sendMessage(request.toCommand(userId));
        return SendMessageResponse.from(result);
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
