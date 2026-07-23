plugins {
    java
    id("org.springframework.boot") version "3.2.1" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "com.chatbot.bravo"
    version = "0.0.1-SNAPSHOT"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.1")
            mavenBom("org.junit:junit-bom:5.14.0")
            mavenBom("org.mockito:mockito-bom:5.20.0")
            mavenBom("org.springframework.ai:spring-ai-bom:1.1.0")
        }
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test") {
            exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        }
        "testImplementation"("org.assertj:assertj-core:3.26.3")
        // junit-bom(5.14)의 jupiter 엔진과 Gradle 번들 launcher의 버전 불일치 방지
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // 시각 저장 기준을 프로덕션(Application.main의 UTC 고정)과 동일하게 —
        // 실행 머신의 타임존에 따라 테스트 결과가 달라지는 것을 차단
        systemProperty("user.timezone", "UTC")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }
}
