# spring-gift-test

## 1. Cucumber BDD 적용

### JUnit Cucumber 돌리는 방법

```bash
./gradlew test
```

### 관련 내용 정리

- [Cucumber BDD 도입 진행 과정](cucumber/claude-chat-history.md)
- [학습한 내용 & 나중에 공부할 내용](cucumber/new-things.md)

## 2. PostgreSQL + Docker Compose 통합

### Docker compose (자동) 과 함께 Cucumber 돌리는 방법

```bash
./gradlew cucumberTest
```

### 관련 내용 정리

- [Docker compose 도입 진행 과정](docker-compose/claude-chat-history.md)

## 3. Application 컨테이너화

### Docker 기반 E2E 테스트 실행 방법

**자동 실행 (빌드 + 기동 + 테스트 + 정리)**:

```bash
./gradlew dockerTest
```

**수동 실행 (단계별)**:

```bash
# 1. Docker 이미지 빌드
./gradlew dockerBuild

# 2. 컨테이너 기동 (PostgreSQL + Application)
./gradlew dockerUp

# 3. 애플리케이션 응답 확인
curl http://localhost:28080

# 4. E2E 테스트 실행
./gradlew dockerTest

# 5. 컨테이너 정리
# 사실 dockerTest 가 finalizedBy 로 dockerDown 을 실행해서 필요 없긴함.
./gradlew dockerDown
```

### 관련 내용 정리

- [Application 컨테이너화 진행 과정](containerize/claude-chat-history.md)
