# spring-gift-test

카카오 연동 선물 관리/배송 시스템.

## 실행

```bash
./gradlew bootRun
```

## 테스트

```bash
./gradlew test
```

Cucumber BDD 기반 인수 테스트가 실행됩니다.

### 테스트 구조

- `src/test/resources/features/` — Gherkin 시나리오 (한글)
- `src/test/java/gift/acceptance/` — Step Definitions, 공통 스텝, Cucumber 설정
- `src/test/java/gift/support/` — Fixture, DB 초기화
