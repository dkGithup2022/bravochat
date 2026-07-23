package com.chatbot.bravo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

import java.util.TimeZone;

@SpringBootApplication
@EnableJdbcAuditing
public class Application {

    public static void main(String[] args) {
        // 시각 저장 규약의 실체 — DATETIME(6)은 타임존이 없어 Instant↔DB 변환이 JVM 기본 타임존을 탄다.
        // 배포 환경과 무관하게 저장 기준을 UTC로 고정 (테스트 JVM도 루트 build.gradle에서 동일 고정)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(Application.class, args);
    }
}
