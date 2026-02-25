# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 빌드 및 테스트 명령어

```bash
./gradlew build          # 빌드 + test (Cucumber 제외)
./gradlew test           # 1단계 인수 테스트 (H2, Cucumber 제외)
./gradlew dockerBuild    # Docker 이미지 빌드
./gradlew dockerUp       # Docker Compose 시작 (postgres + app)
./gradlew cucumberTest   # Cucumber 인수 테스트 (Docker PostgreSQL)
./gradlew dockerDown     # Docker Compose 종료
./gradlew bootRun        # 애플리케이션 실행 (H2 인메모리 DB, 포트 8080)
./gradlew test --tests "gift.SomeTest"              # 단일 테스트 클래스 실행
./gradlew test --tests "gift.SomeTest.methodName"   # 단일 테스트 메서드 실행
```

Java 21 필수. Cucumber 테스트 실행 시 **Docker가 실행 중**이어야 한다.
Docker 소켓은 주요 런타임(Docker Desktop, Colima, OrbStack, Rancher Desktop)을 자동 탐색한다. 탐색 실패 시 `DOCKER_HOST` 환경변수로 지정. 상세 안내는 `README.md` 참조.

## 현재 과제: 요구사항 3 — Application 컨테이너화

### 목표: End-to-End Docker 환경
Spring Boot 애플리케이션 자체도 Docker 컨테이너로 실행하여, 프로덕션과 동일한 환경에서 End-to-End 테스트를 수행한다.

### 아키텍처

```
[요구사항 2] Test JVM → embedded Spring Boot + Testcontainers PostgreSQL
[요구사항 3] Test JVM → Docker App (localhost:28080) + Docker PostgreSQL (localhost:5432)
```

- **Docker App**: `spring-gift-test:latest` 이미지, 포트 28080 (호스트) → 8080 (컨테이너)
- **Docker PostgreSQL**: `postgres:17-alpine`, 포트 5432 (호스트/컨테이너)
- **테스트 JVM**: `webEnvironment=NONE` (내장 서버 없음), RestAssured → `localhost:28080`
- **DB 공유**: app 컨테이너는 Docker 네트워크에서 `postgres:5432` 접속, 테스트는 호스트에서 `localhost:5432` 접속 → 같은 DB

### 제약 조건
- **Docker Compose** (postgres + app) 사용 필수
- 기존 Cucumber/Gherkin 시나리오를 Docker 환경에서 실행
- 기존 1단계 인수 테스트(H2)는 그대로 유지
- Docker 실행이 전제 조건

### Cucumber 테스트 실행 흐름

```bash
./gradlew dockerBuild    # Multi-stage build: JDK builder → JRE runtime
./gradlew dockerUp       # docker compose up -d --wait (healthcheck 대기)
./gradlew cucumberTest   # Test JVM → Docker App (28080) + Docker PG (5432)
./gradlew dockerDown     # docker compose down
```

### Docker 파일 구조

| 파일 | 설명 |
|------|------|
| `Dockerfile` | Multi-stage build (JDK builder → JRE runtime) |
| `.dockerignore` | 빌드 컨텍스트에서 불필요 파일 제외 |
| `docker-compose.yml` | postgres + app 서비스 정의 (healthcheck 포함) |

### Spring 프로파일 분리

| 프로파일 | 용도 | DB |
|---------|------|-----|
| 기본 (`application.properties`) | 개발 + 1단계 테스트 | H2 인메모리 |
| `cucumber` (`application-cucumber.properties`) | Cucumber 테스트 | Docker PostgreSQL (localhost:5432) |

- `CucumberSpringConfig`에 `@ActiveProfiles("cucumber")` 적용
- datasource는 `application-cucumber.properties`에 정적 설정 (`localhost:5432`)
- `spring.jpa.hibernate.ddl-auto=none` (Docker app이 `create`로 스키마 생성)
- `spring.main.web-application-type=none` (내장 서버 비활성화)

### CucumberSpringConfig 구성

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("cucumber")
public class CucumberSpringConfig {
    @io.cucumber.java.Before(order = 1)
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 28080;
    }
}
```

- Testcontainers 코드 제거 (PostgreSQLContainer, static 블록, @DynamicPropertySource)
- `webEnvironment=NONE` — 앱은 Docker에서 실행
- RestAssured → Docker app의 28080 포트

### DB 초기화 (Test Isolation)

`DatabaseCleanup.java`는 PostgreSQL 호환 SQL 사용:
- `TRUNCATE TABLE wish, option, product, member, category CASCADE`
- JdbcTemplate은 Spring 컨텍스트에서 `localhost:5432`(Docker PG)로 자동 연결

### 테스트 설계 원칙: "어떻게 되는가"를 검증한다

인수 테스트는 **내부 구현이 아닌 사용자 입력과 그 결과**에 의존해야 한다. 세부 구현(엔티티 구조, repository 메서드, 서비스 내부 로직)에 의존하는 테스트는 리팩토링 시 깨진다. "어떻게 하는가"가 아니라 **"어떻게 되는가"**를 검증하는 테스트를 작성한다.

### Cucumber 디렉토리 구조

```
src/test/
├── java/gift/
│   ├── cucumber/
│   │   ├── CucumberTest.java              # @Suite 엔트리포인트
│   │   ├── CucumberSpringConfig.java      # @CucumberContextConfiguration + RestAssured 설정
│   │   ├── DatabaseCleanup.java           # @Before(order=0) TRUNCATE CASCADE
│   │   ├── ScenarioState.java             # @ScenarioScope 상태 공유
│   │   └── steps/
│   │       ├── CategorySteps.java
│   │       ├── ProductSteps.java
│   │       └── GiftSteps.java
│   └── AcceptanceTestSupport.java         # 1단계 공통 API 호출 헬퍼
└── resources/
    ├── features/
    │   ├── category.feature
    │   ├── product.feature
    │   └── gift.feature
    ├── application-cucumber.properties    # Cucumber 프로파일 설정 (Docker PG)
    ├── cleanup.sql                        # 1단계 H2 테스트용
    └── test-data.sql                      # 1단계 H2 테스트용
```

### Gherkin 작성 원칙
- **비즈니스 언어로 작성**: 기획자/QA가 읽고 이해할 수 있어야 한다
- **구현 세부사항 노출 금지**: HTTP 메서드, 상태 코드, JSON 필드명 등을 시나리오에 직접 쓰지 않는다
- **한국어 Gherkin 키워드 사용**: `기능`, `시나리오`, `Given`/`When`/`Then` (또는 `주어진`/`만일`/`그러면`)

### 컨트롤러 요청 바인딩
- 모든 POST 엔드포인트(`/api/products`, `/api/categories`, `/api/gifts`)에 `@RequestBody`가 있음 → **JSON body**로 전송

### 검증 전략: 다음 행동으로 이전 행동을 검증
- DB를 직접 조회하지 않고 **API 응답**과 **후속 행위의 성공/실패**로 검증
- 카테고리 생성 → 해당 카테고리로 상품 생성 → 상품 목록 조회에서 카테고리 확인 (시나리오 체이닝)
- 재고 전부 소진하는 선물 → 성공 → 같은 옵션에 재선물 → 실패 (재고 감소 검증)

### 검증 대상 핵심 행위
1. 카테고리 생성 (`POST /api/categories`) — 응답에 id, name 확인
2. 카테고리 목록 조회 (`GET /api/categories`) — 생성한 카테고리가 목록에 존재
3. 상품 생성 (`POST /api/products`) — 응답에 id, name, category 확인
4. 상품 목록 조회 (`GET /api/products`) — 생성한 상품이 목록에 존재
5. 선물하기 성공 (`POST /api/gifts`) — 재고 충분 시 200 응답
6. 선물하기 후 재고 감소 — 재고 전부 소진 후 재시도 시 실패로 검증 (행위 기반)
7. 재고 부족 시 선물 실패 — 재고 초과 수량 요청 시 400 응답

> **WishService**: 컨트롤러가 없으므로 API 레벨 인수 테스트 범위에서 제외.

## 아키텍처

선물하기 플랫폼 (카카오 선물하기 스타일). Spring Boot 3.5, JPA + PostgreSQL, Thymeleaf.

### 패키지 구조 (`src/main/java/gift/`)

- **model/** — JPA 엔티티(`Product`, `Category`, `Option`, `Member`, `Wish`), 리포지토리, 도메인 인터페이스(`GiftDelivery`). `Gift`는 선물 트랜잭션에서만 사용되는 비영속 값 객체.
- **application/** — 서비스 및 요청 DTO. 모든 서비스는 `@Transactional` + 생성자 주입.
- **ui/** — REST 컨트롤러 (`/api/products`, `/api/categories`, `/api/gifts`), `GlobalExceptionHandler` (`IllegalStateException`/`NoSuchElementException` → 400 BAD_REQUEST).
- **infrastructure/** — 인터페이스 구현체 및 외부 설정 프로퍼티. `FakeGiftDelivery`는 현재 `GiftDelivery` 구현체 (콘솔 출력 스텁).

### 핵심 도메인 관계

```
Category 1──N Product 1──N Option
Member 1──N Wish N──1 Product
Member(발신자) + Member(수신자) + Option → Gift (비영속)
```

### 선물하기 흐름 (핵심 비즈니스 로직)

`POST /api/gifts` + `Member-Id` 헤더 → `GiftService.give()` → `Option` 조회, `option.decrease(quantity)` 호출 (재고 부족 시 `IllegalStateException`), `Gift` 값 객체 생성, `GiftDelivery.deliver()` 위임. 재고는 트랜잭션 내 JPA dirty checking으로 자동 반영.

### 확장 포인트

`GiftDelivery` 인터페이스(`model/`)는 선물 배송 전략 패턴. 현재는 `FakeGiftDelivery`(콘솔 출력). 향후 `KakaoMessageProperties` / `KakaoSocialProperties` 설정(`kakao.message.*`, `kakao.social.*`)을 사용한 카카오 API 구현체로 교체 예정.

### 주요 컨벤션

- `open-in-view=false` — 트랜잭션 밖에서 지연 로딩 불가.
- 인증 계층 미구현. 발신자 식별은 `Member-Id` 요청 헤더로 처리.
- `WishService`는 존재하나 대응 컨트롤러 없음.
