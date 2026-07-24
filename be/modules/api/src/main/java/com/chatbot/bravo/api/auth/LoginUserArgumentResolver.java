package com.chatbot.bravo.api.auth;

import com.chatbot.bravo.exception.auth.InvalidSessionException;
import com.chatbot.bravo.model.auth.LoginSession;
import com.chatbot.bravo.service.auth.SessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** @LoginUser 파라미터에 인증된 LoginSession을 주입한다. 등록은 api-application의 WebConfig. */
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionManager sessionManager;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && LoginSession.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        LoginUser annotation = parameter.getParameterAnnotation(LoginUser.class);
        boolean required = annotation == null || annotation.required();

        String sessionKey = parseBearer(webRequest.getHeader(HttpHeaders.AUTHORIZATION));
        if (sessionKey == null) {
            if (required) {
                throw new InvalidSessionException("authorization header missing or malformed");
            }
            return null;
        }

        try {
            return sessionManager.check(sessionKey);   // 검증 + lastRequestedAt touch
        } catch (InvalidSessionException e) {
            if (required) {
                throw e;
            }
            return null;
        }
    }

    /** "Bearer {key}" 형식이 아니면 null. */
    private String parseBearer(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String key = header.substring(BEARER_PREFIX.length()).trim();
        return key.isEmpty() ? null : key;
    }
}
