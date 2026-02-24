# 선물하기 서비스 - E2E 테스트 인프라 가이드

## 현재 상태 (2단계 완료)

- **1단계 완료:** Cucumber BDD 테스트 적용 (한글 Gherkin, Step Definitions, 시나리오 간 데이터 격리)
- **2단계 완료:** Cucumber 테스트 DB를 H2 → PostgreSQL(Docker Compose)로 전환
- Feature 파일: `src/test/resources/features/` (category, product, gift)
- Step Definitions: `src/test/java/gift/cucumber/steps/`
- CucumberSpringConfiguration: `webEnvironment = RANDOM_PORT`, `@LocalServerPort` 사용, PostgreSQL TRUNCATE + CASCADE
- 기존 AcceptanceTest: H2 + `@Sql` 어노테이션으로 동작 중 (변경 없음)
- `docker-compose.yml`: PostgreSQL 16 컨테이너만 정의 (포트 `15432`)
- `application-cucumber.properties`: PostgreSQL 접속 설정 (`ddl-auto=create-drop`)
- `build.gradle`: `cucumberTest` 태스크 (doFirst로 DB 컨테이너 시작) + `composeDown` 태스크

### 현재 테스트 실행 (2단계)

```bash
./gradlew cucumberTest  # PostgreSQL Docker 자동 시작 → Cucumber 테스트 → 자동 종료
./gradlew test          # AcceptanceTest만 (H2, Cucumber 제외, Docker 불필요)
```

---

### 3단계 목표

Spring Boot 애플리케이션을 **Docker 컨테이너**로 실행하고,
테스트 코드는 **호스트**에서 Docker 컨테이너의 애플리케이션에 HTTP 요청을 보내는 구조로 전환한다.

---

## 3단계 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│  Host (테스트 실행 환경)                                       │
│                                                             │
│  Cucumber Test ──HTTP──▶ localhost:28080                     │
│  Cucumber Test ──JDBC──▶ localhost:15432  (DB cleanup용)     │
└─────────────────────────────────────────────────────────────┘
         │                        │
         ▼                        ▼
┌─────────────────────────────────────────────────────────────┐
│  Docker Network                                             │
│                                                             │
│  ┌─────────────┐           ┌─────────────┐                  │
│  │  app         │──JDBC──▶ │  postgres    │                  │
│  │  (Spring Boot)│          │  (PostgreSQL)│                  │
│  │  :8080       │           │  :5432       │                  │
│  └─────────────┘           └─────────────┘                  │
│   ↕ 28080:8080              ↕ 15432:5432                    │
└─────────────────────────────────────────────────────────────┘
```

- **테스트 코드 (Host)** → HTTP → `localhost:28080` → Docker App 컨테이너
- **테스트 코드 (Host)** → JDBC → `localhost:15432` → Docker DB 컨테이너 (TRUNCATE용)
- **App 컨테이너** → JDBC → `postgres:5432` → Docker DB 컨테이너 (Docker 내부 네트워크)

---

## 테스트 실행 (3단계 완료 후)

```bash
./gradlew dockerBuild         # Docker 이미지 빌드
./gradlew dockerUp            # App + DB 컨테이너 시작
curl http://localhost:28080   # 애플리케이션 응답 확인
./gradlew cucumberTest        # Docker 환경에서 Cucumber 테스트 실행
./gradlew dockerDown          # 컨테이너 종료

./gradlew test                # AcceptanceTest만 (H2, Cucumber 제외, Docker 불필요)
```

---

## 테스트 분리 구조

- **기본 `test` 태스크:** `gift/cucumber/**` 패턴을 제외하여 AcceptanceTest만 실행한다. (H2)
- **`cucumberTest` 태스크:** `gift/cucumber/**` 패턴만 포함하여 Cucumber 테스트만 실행한다. (Docker 환경)

---

## 변경 금지 사항

- 기존 `application.properties` 수정 금지
- 기존 `*AcceptanceTest` 파일 수정 금지
- H2 의존성(`com.h2database:h2`) 제거 금지
- Entity 클래스 수정 금지
- Feature 파일(`src/test/resources/features/`) 수정 금지
- Step Definitions(`src/test/java/gift/cucumber/steps/`) 수정 금지

> **3단계 수정 대상**: `CucumberSpringConfiguration.java`, `build.gradle`, `docker-compose.yml`, `application-cucumber.properties`는 3단계에서 수정이 필요하다.

---

## Skills

| Skill | 용도 |
| --- | --- |
| `/acceptance-test-writer` | Cucumber BDD Feature 파일 + Step Definitions 작성 (1단계) |
| `/postgres-docker-setup` | H2 → PostgreSQL + Docker Compose 전환 (2단계) |
| `/app-containerization` | Application Docker 컨테이너화 (3단계) |
