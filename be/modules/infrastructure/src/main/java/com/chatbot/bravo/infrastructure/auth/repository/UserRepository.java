package com.chatbot.bravo.infrastructure.auth.repository;

import com.chatbot.bravo.model.auth.User;

import java.util.Optional;

public interface UserRepository {

    /**
     * username으로 유저 단건 조회. 로그인 시 유저 식별용.
     * 특이사항: username은 유니크 제약. 반환된 User에는 password(해시)가 포함되므로
     * 외부 응답으로 직접 노출하지 말 것.
     */
    Optional<User> findByUsername(String username);
}
