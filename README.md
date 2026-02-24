# spring-gift-test

인수 테스트 중심으로 기능을 검증하며, PostgreSQL + Docker Compose 기반 실행을 지원합니다.

## Acceptance Test Docs

- 테스트 전략 문서: [TEST_STRATEGY.md](./TEST_STRATEGY.md)
- 테스트 실행 가이드: [TEST_RUN_GUIDE.md](./TEST_RUN_GUIDE.md)
- AI 활용 기록: [USING_AI.md](./USING_AI.md)

## 빠른 실행

```bash
./gradlew test         # Cucumber BDD 테스트 실행(H2)
./gradlew cucumberTest # Docker(App+PostgreSQL)로 Cucumber 실행
./gradlew step1Test    # 기존 RestAssured 인수 테스트 실행
```

## 요구사항 3 실행(컨테이너)

```bash
./gradlew dockerBuild
./gradlew dockerUp
curl http://localhost:28080/api/categories
./gradlew cucumberTest
./gradlew dockerDown
```
