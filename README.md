# spring-gift-test

## 테스트 실행 방법

### 1. 단위/기존 테스트 (H2 in-memory)

```bash
./gradlew test
```

### 2. Cucumber BDD + PostgreSQL (Docker Compose)

Docker 이미지를 빌드하고, 전체 환경(App + PostgreSQL)을 띄운 뒤 테스트를 실행합니다.

```bash
# 1) Docker 이미지 빌드
./gradlew dockerBuild

# 2) 전체 서비스 시작 (App + PostgreSQL)
./gradlew dockerUp

# 3) 애플리케이션 응답 확인
curl http://localhost:28080/api/categories

# 4) Cucumber 인수 테스트 실행
./gradlew cucumberTest

# 5) 전체 서비스 종료 및 정리
./gradlew dockerDown
```

### 트러블슈팅

```bash
# 컨테이너 상태 확인
docker ps

# 앱 로그 확인
docker logs spring-gift-test-kakao-app-1

# PostgreSQL 로그 확인
docker logs spring-gift-test-kakao-postgres-1
```
