# spring-gift-test

Spring Boot 기반 선물/상품 관리 시스템

## 요구사항

- Java 21
- Docker & Docker Compose

## 실행 방법

### Docker 기반 실행 (권장)

```bash
# Docker 이미지 빌드
./gradlew dockerBuild

# 전체 시스템 시작 (PostgreSQL + 애플리케이션)
./gradlew dockerUp

# 애플리케이션 응답 확인
curl http://localhost:28080/actuator/health

# Cucumber 테스트 실행 (Docker 컨테이너 대상)
./gradlew cucumberTest

# 전체 시스템 종료
./gradlew dockerDown
```

### 로컬 개발 환경 실행

```bash
# PostgreSQL 시작
docker compose up -d postgres

# 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 테스트 실행

```bash
# Docker 컨테이너 대상 Cucumber 테스트
./gradlew dockerUp
./gradlew cucumberTest

# 내장 서버 + PostgreSQL Cucumber 테스트
./gradlew cucumberLocalTest

# 전체 테스트 (H2 인메모리 DB)
./gradlew test
```

### Docker 관리

```bash
# 이미지 빌드
./gradlew dockerBuild

# 전체 컨테이너 시작
./gradlew dockerUp

# 전체 컨테이너 종료
./gradlew dockerDown

# 볼륨 포함 완전 삭제
docker compose down -v
```

## 프로파일 설정

| 프로파일 | 데이터베이스 | 앱 포트 | 용도 |
|---------|------------|--------|-----|
| (없음) | H2 in-memory | 8080 | 단위 테스트 |
| dev | PostgreSQL (5432) | 8080 | 로컬 개발 |
| test | PostgreSQL (5433) | random | 내장 서버 테스트 |
| docker | PostgreSQL (컨테이너) | 28080 | Docker E2E 테스트 |

## 프로젝트 구조

```
spring-gift-test-kakao/
├── Dockerfile                # Multi-stage 빌드
├── docker-compose.yml        # PostgreSQL + App 정의
├── scripts/
│   └── check-db.sh          # DB 상태 체크 스크립트
├── src/
│   ├── main/
│   │   ├── java/gift/
│   │   │   ├── ui/           # REST 컨트롤러
│   │   │   ├── application/  # 서비스 & DTO
│   │   │   ├── model/        # 엔티티 & Repository
│   │   │   └── infrastructure/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       └── application-docker.properties
│   └── test/
│       ├── java/gift/cucumber/
│       └── resources/
│           ├── application.properties
│           ├── application-test.properties
│           ├── application-docker.properties
│           ├── features/     # Cucumber feature 파일
│           └── sql/          # 테스트 데이터
```
