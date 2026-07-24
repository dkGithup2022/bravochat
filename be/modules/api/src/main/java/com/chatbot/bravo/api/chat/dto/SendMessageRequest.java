package com.chatbot.bravo.api.chat.dto;

import com.chatbot.bravo.service.chat.dto.SendMessageCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대화 요청")
public class SendMessageRequest {

    @NotBlank
    @Size(max = 4000)
    @Schema(description = "사용자 입력 텍스트", example = "안녕하세요")
    private String message;

    public SendMessageCommand toCommand(Long userId) {
        return new SendMessageCommand(userId, message);
    }
}
