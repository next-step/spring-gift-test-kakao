# spring-gift-test

## 프로젝트 소개
Spring Boot 기반 선물하기(Gift) API의 인수 테스트 프로젝트입니다.
Cucumber + RestAssured를 활용한 BDD 스타일 인수 테스트를 작성합니다.

## 기술 스택
- Java 21, Spring Boot 3.5.8, Spring Data JPA, PostgreSQL 17
- Docker Compose (앱 + DB 컨테이너)
- 테스트: Cucumber 7.22.0, RestAssured, JUnit 5, Mockito

## 사전 요구사항
- Java 21
- Docker (Docker Desktop 또는 Colima 등)

## 실행 환경 구성

### 아키텍처

테스트는 Docker Compose로 띄운 외부 컨테이너에 HTTP 요청을 보내고, 같은 Docker DB에 JDBC로 접근하여 데이터 세팅/검증한다.

```
테스트 (Host) → HTTP → localhost:28080 (Docker App 컨테이너)
테스트 (Host) → JDBC → localhost:5432  (Docker DB 컨테이너)
Docker App    → JDBC → db:5432         (Docker DB 컨테이너, 내부 통신)
```

### 개발 서버 실행

```bash
./gradlew bootRun
```

- `docker-compose.yml`의 PostgreSQL(포트 5432)이 자동 시작된다.
- 데이터는 Docker volume(`gift-data`)에 영속 저장된다.

### 테스트 실행

```bash
# Cucumber BDD 테스트 (Docker 빌드 → 시작 → 테스트를 한 번에 실행)
./gradlew cucumberTest

# 테스트 완료 후 컨테이너 종료
./gradlew dockerDown
```

`cucumberTest`는 `dockerUp`에 의존하고, `dockerUp`은 `dockerBuild`에 의존하므로 한 번의 명령으로 전체 워크플로가 실행된다.

```
./gradlew cucumberTest
  └─ dockerUp (컨테이너 시작 + localhost:28080 헬스체크)
       └─ dockerBuild (Docker 이미지 빌드)
```

#### Gradle 태스크 목록

| 태스크 | 그룹 | 설명 |
|---|---|---|
| `./gradlew dockerBuild` | docker | Docker 이미지 빌드 (`docker-compose build`) |
| `./gradlew dockerUp` | docker | 컨테이너 시작 + 앱 준비 대기 (최대 60초 폴링) |
| `./gradlew dockerDown` | docker | 컨테이너 종료 (`docker-compose down`) |
| `./gradlew test` | verification | JUnit 인수 테스트 (Docker 선행 필요) |
| `./gradlew cucumberTest` | verification | Cucumber BDD 테스트 (Docker 자동 실행) |

| 명령어 | DB | 대상 | 테스트 수 | Docker 자동 실행 |
|---|---|---|---|---|
| `./gradlew test` | PostgreSQL (Docker) | JUnit 인수 테스트 | 13개 | O (`dockerBuild` → `dockerUp` 자동) |
| `./gradlew cucumberTest` | PostgreSQL (Docker) | Cucumber BDD 테스트 | 13개 | O (`dockerBuild` → `dockerUp` 자동) |

- `test` 태스크는 `excludeEngines 'cucumber'`와 `exclude '**/CucumberTest*'`로 Cucumber 테스트를 제외한다.
  - `CucumberTest.java`가 `@Suite` (JUnit Platform Suite 엔진)를 사용하므로 엔진 제외만으로는 부족하여 클래스 제외도 필요하다.
- `cucumberTest` 태스크는 `includeEngines 'cucumber'`로 Cucumber 엔진만 실행하며, `cucumber.features`/`cucumber.glue` 시스템 프로퍼티로 feature 파일 경로와 glue 코드 위치를 지정한다.
- Cucumber HTML 리포트: `build/reports/cucumber/cucumber-report.html`

## 테스트 구조

```
src/test/
├── java/gift/
│   ├── CucumberTest.java                  # Cucumber 실행 진입점 (@Suite)
│   ├── CucumberSpringConfiguration.java   # Spring 컨텍스트 + MockitoBean 설정
│   ├── steps/
│   │   ├── SharedContext.java             # 시나리오 간 상태 공유 (@ScenarioScope)
│   │   ├── Hooks.java                     # @Before(포트 설정 + 데이터 정리)
│   │   ├── CategorySteps.java             # 카테고리 관련 Step Definitions
│   │   ├── ProductSteps.java              # 상품 관련 Step Definitions
│   │   └── GiftSteps.java                 # 선물 관련 Step Definitions
│   └── ui/                                # JUnit + RestAssured 인수 테스트
│       ├── CategoryRestControllerTest.java
│       ├── ProductRestControllerTest.java
│       └── GiftRestControllerTest.java
└── resources/
    ├── application-test.yml             # Docker PostgreSQL 연결 프로필
    └── features/
        ├── category.feature               # 카테고리 생성/조회 시나리오 (6개)
        ├── product.feature                # 상품 등록/조회 시나리오 (2개)
        └── gift.feature                   # 선물 보내기 시나리오 (5개)
```

### 테스트 시나리오

시나리오 생명주기:

```
시나리오 시작
├─ @Before: DB 전체 삭제 (자식→부모 순) + RestAssured.port = 28080
├─ SharedContext 새 인스턴스 생성
├─ Step 클래스들 새 인스턴스 생성
├─ Background 실행 (Given 단계)
├─ Scenario 본문 실행 (When/Then)
└─ 시나리오 종료
```

### 카테고리 관리 (category.feature)
- 유효한 이름으로 카테고리를 생성하면 200 OK와 생성된 카테고리를 반환한다
- 생성된 카테고리는 목록 조회 시 포함된다
- 빈 이름으로 카테고리를 생성하면 빈 이름으로 저장된다
- name 필드 누락 시 null로 저장된다
- 카테고리 목록을 조회한다
- 동일한 이름의 카테고리를 여러 개 생성할 수 있다

### 상품 관리 (product.feature)
- 유효한 상품을 등록한다
- 상품 목록을 조회한다

### 선물 보내기 (gift.feature)
- 유효한 요청으로 선물을 보내면 200 OK와 재고가 차감된다
- 존재하지 않는 옵션으로 선물을 보내면 500 에러가 발생한다
- 재고보다 많은 수량을 요청하면 500 에러가 발생한다
- Member-Id 헤더가 없으면 400 에러가 발생한다
- 재고와 동일한 수량을 요청하면 200 OK와 재고가 0이 된다

## 테스트 전략

### 데이터 설정 (Given)

인수 테스트는 사용자 관점에서 작성하므로, 테스트 데이터도 **API 호출**로 생성하는 것을 원칙으로 한다.
API가 존재하지 않는 경우에만 Repository 직접 접근을 허용한다.

| 데이터 | 방식 | 이유 |
|--------|------|------|
| 카테고리 | API 호출 (`POST /api/categories`) | API 존재 |
| 상품 | API 호출 (`POST /api/products`) | API 존재 |
| 옵션 | Repository 직접 접근 | 생성 API 없음 |
| 회원 | Repository 직접 접근 | 생성 API 없음 |

### 검증 (Then)

검증 방법은 다음 우선순위를 따른다.

| 우선순위 | 방법 | 설명 | 적용 예시 |
|---------|------|------|----------|
| 1순위 | 조회 API 활용 | 생성 후 조회 API로 결과를 확인한다 | 카테고리 생성 → 목록 조회로 확인 |
| 2순위 | 실패 시나리오 간접 검증 | 동일 행위를 반복하여 실패하는지 확인한다 | 재고 소진 후 추가 선물 시 실패 |
| 3순위 | DB 직접 확인 | Repository로 DB를 직접 조회하여 검증한다 | 선물 후 옵션 재고 확인 (조회 API 없음) |

### 데이터 격리

- `@Before` 훅에서 `deleteAllInBatch()`로 매 시나리오 시작 전 데이터를 정리한다
  - 이전 시나리오가 실패하더라도 다음 시나리오는 항상 깨끗한 DB에서 시작된다
  - 외래키 제약 순서: Option → Product → Category → Member
- `@ScenarioScope`로 SharedContext를 시나리오마다 새로 생성하여 상태 누출을 방지한다
- RestAssured는 별도 스레드에서 HTTP 요청을 보내므로 `@Transactional` 롤백이 동작하지 않아 수동 삭제가 필요하다

## Spring 프로파일 설정

| 프로파일 | 설정 파일 | DB | 용도 |
|---------|--------|----|------|
| 기본 | `application.yml` | PostgreSQL (Docker Compose, port 5432) | 개발 |
| test | `application-test.yml` | PostgreSQL (Docker, port 5432) | 테스트 |

- 테스트 클래스에 `@ActiveProfiles("test")`가 적용되어 Docker PostgreSQL에 연결한다.
- `ddl-auto: validate` — Docker 앱이 이미 스키마를 생성하므로 검증만 수행한다.
- `docker.compose.enabled: false` — 테스트에서 docker-compose 자동 실행을 방지한다.
