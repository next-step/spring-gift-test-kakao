# spring-gift-test

## 실행 방법

### Cucumber BDD 테스트 (Docker 기반 E2E)

PostgreSQL + 애플리케이션을 Docker 컨테이너로 실행하고, Cucumber 테스트를 수행합니다.
컨테이너 시작/종료가 자동으로 관리됩니다.

```bash
./gradlew cucumberTest
```

### Docker 수동 관리

```bash
./gradlew dockerBuild   # Docker 이미지 빌드
./gradlew dockerUp      # PostgreSQL + 앱 컨테이너 시작
./gradlew dockerDown    # 컨테이너 종료 및 정리
```

### 단위 테스트 (H2 in-memory)

```bash
./gradlew test
```