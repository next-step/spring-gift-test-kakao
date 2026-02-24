# spring-gift-test

카카오 연동 선물 관리/배송 시스템.

## 실행

```bash
./gradlew bootRun    # DB 컨테이너 자동 시작 → dev 프로파일 → gift_dev DB
```

## 종료

```bash
./gradlew dockerDown    # DB 컨테이너 종료
```

## 테스트

```bash
# H2 테스트 (Docker 불필요)
./gradlew test

# Cucumber 인수 테스트 (이미지 빌드 → 컨테이너 시작 → 테스트 → 컨테이너 종료 자동화)
./gradlew cucumberTest
```

### 테스트 구조

- `src/test/resources/features/` — Gherkin 시나리오 (한글)
- `src/test/java/gift/acceptance/` — Step Definitions, 공통 스텝, Cucumber 설정
- `src/test/java/gift/support/` — Fixture, DB 초기화

### DB 환경

| 명령어 | 프로파일 | DB | 실행 환경 |
|--------|----------|-----|-----------|
| `./gradlew bootRun` | dev | PostgreSQL `gift_dev` | Host (embedded) |
| `./gradlew test` | 없음 | H2 (in-memory) | Host (embedded) |
| `./gradlew cucumberTest` | cucumber / docker-test | PostgreSQL `gift_test` | Host (테스트) + Docker (App) |

### cucumberTest 아키텍처

```
테스트 (Host) → HTTP → localhost:28080 (Docker App) → JDBC → db:5432 (Docker PostgreSQL)
테스트 (Host) → JDBC → localhost:5432 (Docker PostgreSQL)  ← DB cleanup
```

- App 컨테이너: `docker-test` 프로파일 → `db:5432/gift_test` 연결, `ddl-auto=update`
- 테스트 프로세스: `cucumber` 프로파일 → `localhost:5432/gift_test` 연결, `ddl-auto=none` (TRUNCATE만 수행)