# chatbot-bravo-be

Spring Boot 3.2.1 / Java 21 / Spring Data JDBC / H2 기반 Hexagonal 멀티모듈 백엔드.

## 실행

./gradlew :modules:applications:api-application:bootRun --args='--spring.profiles.active=local'

- Swagger: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

## 모듈

- applications/api-application — 실행 진입점
- api — Controller / DTO
- service — Reader / Writer / Usecase
- infrastructure — 외부 IO (HTTP/FS/이메일 등)
- repository-jdbc — Spring Data JDBC 구현
- model — Domain model + AuditFields
- schema — DDL (schema.sql)
- exception — 공통 예외
