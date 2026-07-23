plugins {
    `java-library`
}

dependencies {
    api(project(":modules:model"))
    api(project(":modules:exception"))
    api(project(":modules:infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    testImplementation(project(":modules:schema"))
    testRuntimeOnly("com.h2database:h2")
}
