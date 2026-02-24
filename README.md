# spring-gift-test

카카오 연동 선물 관리/배송 시스템.

## 실행

```bash
docker compose up -d       # PostgreSQL 시작
./gradlew bootRun          # dev 프로파일 자동 → gift_dev DB
```

## 테스트

```bash
# H2 테스트 (Docker 불필요)
./gradlew test

# Cucumber 인수 테스트 (PostgreSQL, Docker 자동 시작/종료)
./gradlew cucumberTest
```

### 테스트 구조

- `src/test/resources/features/` — Gherkin 시나리오 (한글)
- `src/test/java/gift/acceptance/` — Step Definitions, 공통 스텝, Cucumber 설정
- `src/test/java/gift/support/` — Fixture, DB 초기화

### DB 환경

| 명령어 | 프로파일 | DB |
|--------|----------|-----|
| `./gradlew bootRun` | dev | PostgreSQL `gift_dev` |
| `./gradlew test` | 없음 | H2 (in-memory) |
| `./gradlew cucumberTest` | cucumber | PostgreSQL `gift_test` |
