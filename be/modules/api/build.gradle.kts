plugins {
    `java-library`
}

dependencies {
    api(project(":modules:model"))
    api(project(":modules:service"))
    api(project(":modules:exception"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
}
