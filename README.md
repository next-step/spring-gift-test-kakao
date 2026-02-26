# spring-gift-test

선물하기 플랫폼 API 서버. 사용자가 카테고리와 상품을 관리하고, 위시리스트를 구성하며, 다른 회원에게 선물을 보낼 수 있다.

## 기술 스택

Java 21 / Spring Boot 3.5.8 / Spring Data JPA / Gradle / H2 / PostgreSQL

## 실행

```bash
./gradlew bootRun
```

## 테스트

```bash
./gradlew test                    # JUnit 단위 테스트 (H2)
./gradlew cucumberTest            # BDD 테스트 (H2)
./gradlew cucumberPostgresTest    # BDD 테스트 (Docker PostgreSQL)
./gradlew cucumberContainerTest   # BDD 테스트 (Docker PostgreSQL + Docker App)
```

> `cucumberPostgresTest`와 `cucumberContainerTest`는 Docker가 필요합니다.

## 문서

| 문서 | 내용 |
|------|------|
| [FEATURES.md](FEATURES.md) | 핵심 기능 명세서 |
| [TEST_STRATEGY.md](TEST_STRATEGY.md) | 테스트 전략 (검증 대상, 우선순위, 테스트 설계) |
| [CUCUMBER_BDD_GUIDE.md](CUCUMBER_BDD_GUIDE.md) | Cucumber BDD 개념 정리 및 적용 가이드 |
| [POSTGRESQL_DOCKER_GUIDE.md](POSTGRESQL_DOCKER_GUIDE.md) | PostgreSQL + Docker Compose 도입 가이드 |
| [APP_CONTAINERIZATION_GUIDE.md](APP_CONTAINERIZATION_GUIDE.md) | Application 컨테이너화 가이드 |
| [AI_USAGE.md](AI_USAGE.md) | AI 활용 방법 및 프롬프트 기록 |