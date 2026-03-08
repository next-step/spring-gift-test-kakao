# spring-gift-test

## 사전 요구사항

- Java 21
- Docker (Cucumber 테스트 실행 시 필요)

## 테스트 실행 방법

### 기존 인수 테스트 (H2)
```bash
./gradlew test
```
RestAssured 인수 테스트 3개를 H2 인메모리 DB로 실행합니다.

### Cucumber 테스트 (Docker 환경)
```bash
./gradlew cucumberTest
```
Docker Compose로 PostgreSQL + 애플리케이션 컨테이너를 자동 시작하고, Cucumber 시나리오 8개를 실행한 후 컨테이너를 자동 종료합니다.

### Cucumber 시나리오 구조
- 시나리오 정의: `src/test/resources/features/*.feature`
- Step 구현: `src/test/java/gift/acceptance/cucumber/`