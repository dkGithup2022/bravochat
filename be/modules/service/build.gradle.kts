plugins {
    `java-library`
}

dependencies {
    api(project(":modules:model"))
    api(project(":modules:exception"))
    api(project(":modules:infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-tx")
    // BCrypt 해시용 단일 모듈 — Spring Security 전체(필터체인/자동설정)가 아님
    implementation("org.springframework.security:spring-security-crypto")

    // IntegrationConfig 가 EnableJdbcAuditing/EnableJdbcRepositories 를 import,
    // 통합 테스트에서 JDBC 어댑터·H2 가 실제로 필요하므로 선제 포함.
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    testImplementation("com.h2database:h2")
    testImplementation(project(":modules:repository-jdbc"))
    testImplementation(project(":modules:schema"))
}
