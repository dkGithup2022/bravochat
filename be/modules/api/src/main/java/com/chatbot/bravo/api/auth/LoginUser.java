package com.chatbot.bravo.api.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증된 로그인 세션을 주입받는 파라미터 표시 — 선언 자체가 "이 API는 인증 필요"의 명시.
 * {@link LoginUserArgumentResolver}가 Authorization 헤더를 검증(SessionManager.check — touch 포함)해 주입한다.
 *
 * <p>required=true(기본): 헤더 없음/형식 오류/세션 없음/만료 → InvalidSessionException(401)
 * <p>required=false: 위 경우 null 주입 — 로그인 여부로 분기하는 비즈니스용.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {

    boolean required() default true;
}
