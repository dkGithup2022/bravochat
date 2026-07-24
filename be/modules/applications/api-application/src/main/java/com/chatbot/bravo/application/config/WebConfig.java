package com.chatbot.bravo.application.config;

import com.chatbot.bravo.api.auth.LoginUserArgumentResolver;
import com.chatbot.bravo.model.auth.LoginSession;
import com.chatbot.bravo.service.auth.SessionManager;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    static {
        // @LoginUser LoginSession 파라미터가 Swagger 문서에 요청 파라미터로 노출되지 않게
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(LoginSession.class);
    }

    private final SessionManager sessionManager;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginUserArgumentResolver(sessionManager));
    }
}
