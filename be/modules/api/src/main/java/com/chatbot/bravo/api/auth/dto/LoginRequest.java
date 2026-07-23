package com.chatbot.bravo.api.auth.dto;

import com.chatbot.bravo.service.auth.dto.LoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 요청")
public class LoginRequest {

    @NotBlank
    @Schema(description = "아이디", example = "tester")
    private String username;

    @NotBlank
    @Schema(description = "비밀번호", example = "password1234")
    private String password;

    public LoginCommand toCommand() {
        return new LoginCommand(username, password);
    }
}
