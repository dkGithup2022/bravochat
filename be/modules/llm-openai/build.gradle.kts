plugins {
    `java-library`
}

dependencies {
    // LlmClient 포트 + 메시지 값 타입 구현 대상
    implementation(project(":modules:infrastructure"))
    implementation(project(":modules:model"))

    // Spring AI (OpenAI) — ChatModel/OpenAiChatOptions 및 자동설정. api-application 런타임에도 전이 필요.
    // 버전은 루트 subprojects 의 spring-ai-bom:1.1.0 이 관리.
    api("org.springframework.ai:spring-ai-starter-model-openai")

    // Jackson (ObjectMapper/JsonMapper/TypeReference) — 응답 JSON 파싱
    implementation("org.springframework.boot:spring-boot-starter-json")
}
