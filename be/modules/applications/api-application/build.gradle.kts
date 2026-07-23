plugins {
    id("org.springframework.boot")
    application
}

dependencies {
    implementation(project(":modules:api"))
    implementation(project(":modules:service"))
    implementation(project(":modules:repository-jdbc"))
    implementation(project(":modules:infrastructure"))
    implementation(project(":modules:schema"))
    implementation(project(":modules:exception"))
    implementation(project(":modules:llm-openai"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    runtimeOnly("com.h2database:h2")
}

application {
    mainClass.set("com.chatbot.bravo.Application")
}

tasks.named<Jar>("jar") { enabled = false }
// bootJar 은 boot 플러그인 기본 구성으로 활성화됨 — 명시적 타입 참조 불필요
