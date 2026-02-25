# spring-gift-test

## 실행 방법

### 단위/행동 테스트 (H2, Docker 불필요)

```bash
./gradlew test
```

RestAssured 기반 행동 테스트를 H2 인메모리 DB로 실행합니다.

### Cucumber BDD 테스트 (Docker 환경, 자동)

```bash
./gradlew cucumberTest
```

Docker Compose가 자동으로 PostgreSQL + App을 시작하고, Cucumber 시나리오를 실행한 뒤 정리합니다.

### Docker 기반 수동 실행

```bash
./gradlew dockerBuild                      # 이미지 빌드
./gradlew dockerUp                         # 전체 시스템 시작 (PostgreSQL + App)
curl http://localhost:28080/api/categories  # 애플리케이션 응답 확인
./gradlew cucumberTest                     # Docker 환경에서 테스트
./gradlew dockerDown                       # 전체 시스템 종료
```

### 테스트 구성

| 명령어 | DB | 대상 | 설명 |
|---|---|---|---|
| `./gradlew test` | H2 | RestAssured 테스트 (7개) | Docker 불필요, 빠른 피드백 |
| `./gradlew cucumberTest` | PostgreSQL (Docker) | Cucumber 시나리오 (7개) | Docker 환경 E2E 테스트 |

| 종류 | 위치 |
|---|---|
| Cucumber Feature 파일 | `src/test/resources/features/` |
| Step Definitions | `src/test/java/gift/cucumber/` |
| RestAssured 테스트 | `src/test/java/gift/` |

### 요구사항

- Java 21
- Gradle Wrapper 포함 (별도 설치 불필요)
- Docker Desktop (cucumberTest 실행 시 필요)