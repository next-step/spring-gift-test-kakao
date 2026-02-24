# spring-gift-test

## 실행 방법

### 전제 조건

- Java 21
- Docker 실행 중

### 빌드

```bash
./gradlew clean build -x test
```

### 테스트

```bash
# 단위 테스트 (H2, Docker 불필요)
./gradlew test

# 인수 테스트 (Docker 자동 기동/정리)
./gradlew cucumberTest
```

## 테스트 아키텍처

```
[Host - Gradle Test Runner]
  │
  ├── RestAssured ──── localhost:28080 ──→ [App 컨테이너 :8080]
  │   (API 호출)                                │
  │                                             │ Docker Network
  │                                             ▼
  └── JdbcTemplate ── localhost:25432 ──→ [PostgreSQL 컨테이너 :5432]
      (데이터 셋업/검증)
```

- `./gradlew test` : 내장 H2로 RestAssured 테스트 실행 (Docker 불필요)
- `./gradlew cucumberTest` : Docker로 PostgreSQL + App 컨테이너를 띄우고 Cucumber BDD 테스트 실행
