plugins {
    `java-library`
}

dependencies {
    api(project(":modules:model"))
    api(project(":modules:exception"))
    implementation("org.springframework.boot:spring-boot-starter")
}
