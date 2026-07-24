package com.chatbot.bravo.jdbc.auth.repository;

import com.chatbot.bravo.infrastructure.auth.repository.UserRepository;
import com.chatbot.bravo.model.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@ComponentScan(basePackages = "com.chatbot.bravo.jdbc.auth.repository")
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class UserJdbcRepositoryTest {

    @Autowired
    private UserRepository userRepository;              // 테스트 대상 (인터페이스로 주입)

    @Autowired
    private UserEntityRepository userEntityRepository;  // 픽스처 생성용 (인터페이스엔 save 없음 — 유저는 시드 전제)

    @BeforeEach
    void setUp() {
        userEntityRepository.save(UserEntity.from(User.create("tester", "hashed-password-1")));
        userEntityRepository.save(UserEntity.from(User.create("other", "hashed-password-2")));
    }

    @Test
    @DisplayName("[성공] 저장된 유저 중 username이 정확히 일치하는 유저를 반환한다")
    void should_returnMatchingUser_when_foundByUsername() {
        Optional<User> found = userRepository.findByUsername("tester");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("tester");
        assertThat(found.get().getPassword()).isEqualTo("hashed-password-1");
    }

    @Test
    @DisplayName("[스펙] 반환된 User는 전 필드가 온전하다 — id 채번 + auditing(createdAt/updatedAt) 자동 세팅")
    void should_returnCompleteUser_when_foundByUsername() {
        User user = userRepository.findByUsername("tester").orElseThrow();

        assertThat(user.getUserId()).isNotNull().isPositive();   // DB 채번
        assertThat(user.getUsername()).isEqualTo("tester");
        assertThat(user.getPassword()).isEqualTo("hashed-password-1");
        assertThat(user.getCreatedAt()).isNotNull();             // @CreatedDate — auditing 미설정 시 null 되는 지점
        assertThat(user.getUpdatedAt()).isNotNull();             // @LastModifiedDate
    }

    @Test
    @DisplayName("[실패] 존재하지 않는 username이면 empty를 반환한다 (예외 변환은 usecase 책임)")
    void should_returnEmpty_when_usernameDoesNotExist() {
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    @DisplayName("[실패/경계] soft-delete된 유저는 조회에서 제외된다 (IsDeletedFalse 필터)")
    void should_returnEmpty_when_userIsSoftDeleted() {
        UserEntity entity = userEntityRepository.findByUsernameAndIsDeletedFalse("tester").orElseThrow();
        userEntityRepository.save(entity.softDelete());

        assertThat(userRepository.findByUsername("tester")).isEmpty();
    }
}
