# spring-gift-test

## 실행 환경

- Java 21
- Spring Boot 3
- Gradle (Groovy DSL)
- Docker (Colima 또는 Docker Desktop)

## 테스트 실행 방법

### Cucumber 인수 테스트 (PostgreSQL)

아래 한 줄로 PostgreSQL 컨테이너 실행부터 테스트, 정리까지 자동으로 수행됩니다.

```bash
./gradlew cucumberTest
```

**자동 수행 흐름:**
1. `docker-compose up -d` — PostgreSQL 16 컨테이너 시작
2. DB 준비 대기 (`pg_isready`)
3. Cucumber 테스트 실행 (PostgreSQL 사용)
4. `docker-compose down` — 컨테이너 정리 (테스트 실패 시에도 실행)

### 사전 준비

Docker 런타임이 실행 중이어야 합니다.

```bash
# Docker Desktop 사용 시
# Docker Desktop 앱 실행

# Colima 사용 시
colima start
```

### 단위 테스트

```bash
./gradlew test
```
