plugins {
    `java-library`
}

dependencies {
    // 도메인 검증 실패를 도메인 예외(4xx 시맨틱)로 직접 던지기 위한 의존
    api(project(":modules:exception"))
}
