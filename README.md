# Spring Gift Service

Spring Boot 기반 선물하기 서비스 애플리케이션입니다.

## 테스트 소개

Cucumber + Gherkin 기반 E2E 인수 테스트로 3개 API, 13개 시나리오를 검증합니다.

| API | 시나리오 | 검증 내용 |
|-----|---------|-----------|
| POST/GET `/api/categories` | 4개 | 생성, DB 저장 확인, 빈 목록, N개 목록 조회 |
| POST/GET `/api/products` | 4개 | 생성, 존재하지 않는 카테고리, 빈 목록, N개 목록 조회 |
| POST `/api/gifts` | 5개 | 선물 전송 성공(재고 감소), 헤더 누락, 없는 옵션, 재고 부족, 발신자 미존재 |

## 테스트 실행 방법

3가지 모드로 동일한 테스트를 실행할 수 있습니다.

### 1. H2 모드 (가장 빠름, Docker 불필요)

H2 인메모리 DB + in-process 앱으로 실행합니다.

```bash
./gradlew test
```

### 2. Testcontainers 모드 (Docker 엔진만 필요)

Testcontainers가 PostgreSQL 컨테이너를 자동으로 띄우고, in-process 앱으로 실행합니다.

```bash
./gradlew cucumberTestAlone
```

### 3. Docker Compose 모드 (전체 Docker 환경)

Docker Compose로 PostgreSQL + 앱 컨테이너를 띄우고, 컨테이너 앱에 요청합니다.

```bash
# 1. Docker 이미지 빌드 (JAR 빌드 포함)
./gradlew dockerBuild

# 2. 컨테이너 실행 (postgres + app)
./gradlew dockerUp

# 3. 테스트 실행
./gradlew cucumberTest

# 4. 컨테이너 정리
./gradlew dockerDown
```

### 모드 비교

| | `./gradlew test` | `./gradlew cucumberTestAlone` | `./gradlew cucumberTest` |
|---|---|---|---|
| DB | H2 (in-memory) | PostgreSQL (Testcontainers) | PostgreSQL (Docker Compose) |
| 앱 | in-process | in-process | Docker 컨테이너 |
| Docker | 불필요 | 엔진만 필요 | 전체 필요 (`dockerBuild` + `dockerUp`) |
| 용도 | 로컬 개발, CI 기본 | DB 호환성 확인 | 배포 환경과 동일한 검증 |
