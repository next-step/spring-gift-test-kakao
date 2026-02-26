# Application 컨테이너화 진행 과정 (Claude code 사용 내역)

## 목표

Spring Boot 애플리케이션을 Docker 컨테이너로 실행하여, 프로덕션과 동일한 환경에서 E2E 테스트를 수행한다.

---

## 진행 사항

### 1. application.dockerfile 작성

Multi-stage build로 구성:

- **Stage 1 (Build)**: `gradle:jdk21-alpine` 이미지에서 소스 코드를 복사하고 `gradle bootJar`로 jar 파일 생성
    - 빌드 설정 파일(`build.gradle`, `settings.gradle`)을 먼저 복사 후 의존성 다운로드 (Docker 레이어 캐싱 활용)
    - 이후 `src/` 복사 및 빌드 실행 (테스트 스킵)
- **Stage 2 (Run)**: `eclipse-temurin:21-jre-alpine` 경량 이미지에서 jar만 복사하여 실행

**의사결정**: 처음 `eclipse-temurin:21-jdk-alpine` + `gradlew`로 작성했으나, 사용자 요구에 따라 Gradle 공식 이미지(`gradle:jdk21-alpine`) 기반으로 변경.

### 2. docker-compose.docker.yaml 검토 및 수정

사용자가 기본 구성을 작성한 뒤, 아래 항목을 검토/반영:

| # | 항목                     | 상태    | 설명                                                          |
|---|------------------------|-------|-------------------------------------------------------------|
| 1 | DATABASE_URL 호스트명      | 반영 완료 | `localhost` → `postgresql` (Docker 네트워크에서 서비스 이름이 hostname) |
| 2 | PostgreSQL healthcheck | 반영 완료 | `pg_isready` + `depends_on: condition: service_healthy`     |
| 3 | 포트 매핑                  | 반영 완료 | `ports: - "28080:8080"` (호스트 28080 → 컨테이너 8080)             |
| 4 | Spring Profile 활성화     | 반영 완료 | `docker.env`에 `SPRING_PROFILES_ACTIVE=docker` 추가            |
| 5 | env_file 참조            | 반영 완료 | application 서비스에 `env_file: docker.env` 추가                  |
| 6 | networks 선언            | 미사용   | 서비스가 2개뿐이라 Docker Compose 기본 네트워크로 충분                       |

### 3. Gradle 자동화 task 구성

`build.gradle`에 Docker 관련 4개 task를 등록:

| task          | 역할                                                                                         |
|---------------|--------------------------------------------------------------------------------------------|
| `dockerBuild` | `docker compose build` — 앱 이미지 빌드                                                          |
| `dockerUp`    | `docker compose up -d --wait` — 컨테이너 기동 (`dockerBuild`에 의존)                                |
| `dockerDown`  | `docker compose down` — 컨테이너 정리                                                            |
| `dockerTest`  | `**/docker/**` 패키지만 실행 + `docker-test` 프로파일 (`dockerUp`에 의존, `dockerDown`으로 `finalizedBy`) |

**실행 흐름**:

```
./gradlew dockerTest
  1. dockerBuild  → docker compose build (이미지 빌드)
  2. dockerUp     → docker compose up -d --wait (PostgreSQL + App 기동, healthy 대기)
  3. Test 실행     → docker-test 프로파일 + docker.env 로드 → localhost:28080에 HTTP 요청
  4. dockerDown   → docker compose down (항상 실행, finalizedBy)
```

### 4. Docker 테스트 설정 (`gift.docker` 패키지)

기존 `gift.cucumber` 패키지와 동일한 구조로 Docker 전용 테스트 설정을 구성:

| 파일                                       | 역할                                                                           |
|------------------------------------------|------------------------------------------------------------------------------|
| `AbstractCucumberDockerFeatureTest.java` | 추상 클래스 — glue 패키지를 `gift.docker, gift.step`으로 설정                             |
| `CucumberDockerSpringConfig.java`        | `@SpringBootTest(NONE)` + `@ActiveProfiles("docker-test")` — 앱 서버를 직접 띄우지 않음 |
| `CucumberDockerHooks.java`               | RestAssured를 `http://localhost:28080`으로 설정 (컨테이너 앱에 요청)                      |
| `*CucumberDockerFeatureTest.java` (3개)   | Feature별 `@Suite` Runner                                                     |

**아키텍처**:

```
테스트 (Host) → HTTP  → localhost:28080 (Docker App)    — API 요청
테스트 (Host) → JDBC  → localhost:5432  (Docker DB)     — 데이터 setup/cleanup
App (Container) → JDBC → postgresql:5432 (Docker DB)    — 앱 내부 DB 접근
```

### 5. Docker 개념 학습

- **Docker Networks**: 컨테이너 간 가상 네트워크 구성. Docker Compose는 별도 설정 없이 기본 네트워크를 자동 생성하며, 서비스 이름이 hostname이 된다.
- **restart 정책**: 컨테이너 종료 시 재시작 여부 결정. `no`(기본), `always`, `on-failure`, `unless-stopped` 4가지 옵션. 테스트 인프라에는 기본값(`no`)이 적절하다.
- **Multi-stage build**: Builder stage(빌드 도구 포함, 수백MB)와 Runtime stage(JRE만, 경량)를 분리하여 최종 이미지 크기를 최소화한다.

---

## 6. 코드 리뷰 및 요구사항 검증

전체 3개 요구사항(Cucumber BDD, PostgreSQL + Docker Compose, Application 컨테이너화)에 대한 코드 리뷰와 요구사항 검증을 수행했다.

### 검증 결과 요약

| 요구사항                           | 판정           |
|--------------------------------|--------------|
| 1. Cucumber BDD                | 충족           |
| 2. PostgreSQL + Docker Compose | 충족           |
| 3. Application 컨테이너화           | 코드 충족, 문서 미비 |

### 발견된 이슈 및 조치

| # | 이슈                                   | 심각도        | 조치                              |
|---|--------------------------------------|------------|---------------------------------|
| 1 | `initAll()`에서 Gift/Wish 테이블 미정리      | 잠재적 위험     | 이번 작업에서 제외 (현재 테스트 통과 확인됨)      |
| 2 | README 섹션 3 미작성                      | 중요         | 작업 7에서 보완                       |
| 3 | `.dockerignore` 미존재                  | 중요         | 작업 7에서 추가                       |
| 4 | `container_name` 충돌 가능성              | 경미         | 이번 작업에서 제외 (순차 실행이므로 실질적 문제 없음) |
| 5 | `restart: always` (cucumber compose) | 경미         | 작업 7에서 제거                       |
| 6 | 재고 부족 시 HTTP 500                     | 프로덕션 코드 이슈 | 이번 작업에서 제외 (리팩토링 단계에서 개선 대상)    |

---

## 7. 마무리 보완 작업

### `.dockerignore` 추가

Docker 빌드 시 context 디렉토리 전체가 Docker daemon에 전송된다. `.dockerignore`가 없으면 `.git/`(수십~수백MB), `build/`(빌드 캐시), `*.md` 문서 등 불필요한 파일이 포함되어:

- 빌드 context 전송 시간 증가
- Docker 레이어 캐싱 무효화 (관련 없는 파일 변경 시에도)
- 최종 이미지에 불필요한 파일 잔류 가능성

```
.git
.gradle
build
*.md
*.env
docker-compose*.yaml
cucumber/
docker-compose/
containerize/
.claude/
```

### `restart: always` 제거 (`docker-compose.cucumber.yaml`)

테스트 인프라는 `./gradlew cucumberTest` 한 사이클에서만 사용되며, `finalizedBy cucumberComposeDown`으로 항상 정리된다. `restart: always`는 프로덕션 서비스 용도이며, 테스트용 컨테이너에는 불필요하다.

### README 섹션 3 보완

Docker 기반 E2E 테스트 실행 방법(자동/수동)과 학습 내용 링크를 추가했다.

---

## 최종 파일 구조

```
프로젝트 루트
├── application.dockerfile           ← Multi-stage Dockerfile
├── docker-compose.docker.yaml       ← PostgreSQL + Application 컨테이너 정의
├── docker.env                       ← Docker 환경변수 (DB 계정, Spring 프로파일)
├── .dockerignore                    ← Docker 빌드 제외 목록
├── build.gradle                     ← dockerBuild/dockerUp/dockerDown/dockerTest task
├── README.md                        ← 섹션 3에 Docker 실행 방법 추가
└── src/
    ├── main/resources/
    │   └── application-docker.yaml  ← 컨테이너 내부 Spring 설정 (postgresql:5432)
    └── test/
        ├── resources/
        │   └── application-docker-test.yaml  ← 테스트 JVM Spring 설정 (localhost:5432)
        └── java/gift/docker/
            ├── AbstractCucumberDockerFeatureTest.java
            ├── CucumberDockerSpringConfig.java   ← @SpringBootTest(NONE)
            ├── CucumberDockerHooks.java          ← RestAssured → localhost:28080
            └── *CucumberDockerFeatureTest.java   ← @Suite Runner (3개)
```

---

## 완료된 작업

- [x] `application.dockerfile` 작성 (Multi-stage build)
- [x] `docker-compose.docker.yaml` 구성 (PostgreSQL + Application)
- [x] `docker.env` 환경변수 파일 구성
- [x] `application-docker.yaml` Spring 프로파일 설정
- [x] `application-docker-test.yaml` 테스트용 Spring 프로파일 설정
- [x] Docker 테스트 설정 (`gift.docker` 패키지)
- [x] Gradle 자동화 task 구성 (`dockerBuild`, `dockerUp`, `dockerDown`, `dockerTest`)
- [x] `networks` 섹션 — 미사용 (기본 네트워크로 충분)
- [x] 코드 리뷰 및 요구사항 검증 수행
- [x] `.dockerignore` 추가
- [x] `restart: always` 제거 (`docker-compose.cucumber.yaml`)
- [x] README 섹션 3 보완
