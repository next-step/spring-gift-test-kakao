## 대화 요약

### 요약 주제
Spring Boot 애플리케이션 Docker 컨테이너화 및 E2E 테스트 환경 구축

### 핵심 내용
- Spring Boot 앱을 Docker 컨테이너로 실행하여 프로덕션과 동일한 환경에서 E2E 테스트를 수행하도록 전환함
- Multi-stage build Dockerfile을 작성하고 docker-compose.yml에 app 서비스를 추가함
- `WebEnvironment.NONE`으로 전환하여 테스트 JVM에서는 웹 서버를 띄우지 않고 Docker 앱에 HTTP 요청함
- Gradle 태스크(`dockerBuild`/`dockerUp`/`dockerDown`)로 전체 시스템 빌드/시작/종료를 자동화함

---

### 1단계: Dockerfile 작성

#### 선택: Multi-stage build

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src
RUN ./gradlew bootJar -x test

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- Stage 1에서 JDK로 빌드, Stage 2에서 JRE로 경량 실행 이미지 생성
- `curl`은 healthcheck용으로 설치

---

### 2단계: Docker Compose 확장

#### 변경: app 서비스 추가 + postgres healthcheck 추가

- `app` 서비스: `28080:8080` 포트 매핑, 환경 변수로 PostgreSQL 접속 정보 주입
- Docker 내부 네트워크에서 `postgres:5432`로 DB 접근
- `depends_on: service_healthy`로 postgres 준비 후 app 시작
- healthcheck URL을 `/api/categories`로 설정 (루트 경로는 404를 반환하여 unhealthy로 판정되었음)

---

### 3단계: WebEnvironment.NONE 전환

#### 선택: NONE + test.server.port

Docker 앱에 HTTP 요청하는 방식으로 3가지 접근법을 검토함:

| 방법 | 설명 |
|------|------|
| A: 프로파일 분기 | 로컬/Docker 모두 지원, 복잡도 높음 |
| B: 별도 러너 | 설정 코드 중복 |
| C: Docker 전용 (NONE) | 단순함, 프로덕션 동일 환경 목표에 부합 |

**방법 C를 선택함.**

- `@SpringBootTest(webEnvironment = WebEnvironment.NONE)`: 테스트 JVM에서 웹 서버를 시작하지 않음
- `test.server.port=28080`: Docker 앱의 포트를 `ApiClient`에 주입
- `local.server.port`는 `NONE`에서 존재하지 않으므로 `test.server.port`로 대체
- Spring 컨텍스트는 로드되므로 JPA Repository 통한 DB 직접 접근(데이터 초기화, 검증)은 유지됨

---

### 4단계: Gradle 태스크 자동화

#### 변경: 태스크 구조 재편

```
./gradlew test          → startDb(postgres만) → 단위/통합 테스트 → stopDb
./gradlew cucumberTest  → dockerBuild → dockerUp(postgres+app) → E2E 테스트 → dockerDown
```

- `startDb`를 `docker compose up postgres`로 변경하여 postgres 서비스만 시작
- `test` 태스크에서 `exclude '**/acceptance/**'`로 Cucumber 테스트 제외
- `cucumberTest`가 `dockerUp`에 의존하고 `dockerDown`으로 종료
- `dockerUp`이 `dockerBuild`에 의존하여 항상 최신 이미지 사용

---

### 결정사항

| 항목 | 선택 | 대안 | 선택 이유 |
|------|------|------|----------|
| WebEnvironment | `NONE` | `RANDOM_PORT` + 프로파일 분기 | Docker 앱에 요청하므로 로컬 웹 서버 불필요 |
| 테스트 → 앱 포트 | `test.server.port=28080` | `local.server.port` | NONE에서는 local.server.port 미존재 |
| healthcheck URL | `/api/categories` | `/` (루트) | 루트 경로에 핸들러 없어 404 반환, 기존 API 엔드포인트 사용 |
| test 태스크 | acceptance 패키지 제외 | 엔진 필터링 | Cucumber가 NONE 모드여서 Docker 없이 실행 불가 |
| cucumberTest 의존 | `dockerUp` → `dockerDown` | `startDb` → `stopDb` | E2E 테스트는 앱 컨테이너 필요 |
| Docker 이미지 빌드 | Multi-stage (JDK → JRE) | 단일 스테이지 | 경량 실행 이미지 생성 |
