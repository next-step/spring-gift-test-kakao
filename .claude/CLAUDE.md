# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 빌드 및 테스트 명령어

```bash
./gradlew build          # 빌드 + test (Cucumber 제외)
./gradlew test           # 1단계 인수 테스트 (H2, Cucumber 제외)
./gradlew cucumberTest   # Cucumber 인수 테스트 (PostgreSQL + Testcontainers, Docker 필수)
./gradlew bootRun        # 애플리케이션 실행 (H2 인메모리 DB, 포트 8080)
./gradlew test --tests "gift.SomeTest"              # 단일 테스트 클래스 실행
./gradlew test --tests "gift.SomeTest.methodName"   # 단일 테스트 메서드 실행
```

Java 21 필수. `cucumberTest` 실행 시 **Docker가 실행 중**이어야 한다.
Docker 소켓은 주요 런타임(Docker Desktop, Colima, OrbStack, Rancher Desktop)을 자동 탐색한다. 탐색 실패 시 `DOCKER_HOST` 환경변수로 지정. 상세 안내는 `README.md` 참조.

## 현재 과제: 요구사항 2 — PostgreSQL + Testcontainers 통합

### 목표: Production Parity
테스트 DB를 프로덕션과 동일한 **PostgreSQL**로 전환하여 DB 방언 차이로 인한 문제를 사전에 방지한다. **Testcontainers**로 PostgreSQL 컨테이너를 자동 관리하여 로컬 DB 설치 없이 테스트를 실행한다.

### 제약 조건
- **Testcontainers + PostgreSQL** 사용 필수
- 기존 2단계 Cucumber/Gherkin 시나리오를 PostgreSQL 위에서 실행
- 기존 1단계 인수 테스트(H2)는 그대로 유지
- Docker 실행이 전제 조건

### 기술 스택

#### Gradle 의존성

```groovy
// 기존 (유지)
runtimeOnly 'com.h2database:h2'                                          // 1단계 테스트 + 개발용
testImplementation 'io.rest-assured:rest-assured'
testImplementation 'io.cucumber:cucumber-java:7.22.1'
testImplementation 'io.cucumber:cucumber-spring:7.22.1'
testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.22.1'
testImplementation 'org.junit.platform:junit-platform-suite'

// 추가 (PostgreSQL + Testcontainers)
testImplementation 'org.testcontainers:postgresql'
testImplementation 'org.testcontainers:junit-jupiter'
testRuntimeOnly 'org.postgresql:postgresql'
```

Testcontainers BOM은 Spring Boot의 `dependency-management` 플러그인이 자동 관리.

#### cucumberTest Gradle task

```groovy
tasks.register('cucumberTest', Test) {
    useJUnitPlatform()
    include 'gift/cucumber/CucumberTest.class'
    group = 'verification'
    description = 'Cucumber 인수 테스트 (PostgreSQL + Testcontainers)'
}
```

`CucumberTest.class`만 포함하도록 필터링 (Cucumber 엔진은 JUnit Jupiter의 `@Tag`를 지원하지 않음).

### Spring 프로파일 분리

| 프로파일 | 용도 | DB |
|---------|------|-----|
| 기본 (`application.properties`) | 개발 + 1단계 테스트 | H2 인메모리 |
| `cucumber` (`application-cucumber.properties`) | Cucumber 테스트 | PostgreSQL (Testcontainers) |

- `CucumberSpringConfig`에 `@ActiveProfiles("cucumber")` 적용
- datasource URL/username/password는 `@DynamicPropertySource`로 Testcontainers가 동적 주입

### Testcontainers 통합 방식

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("cucumber")
public class CucumberSpringConfig {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        postgres.start();  // Cucumber 엔진은 JUnit Jupiter가 아니므로 수동 시작
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

- `static` 초기화 블록으로 컨테이너 1회 시작 (Cucumber는 `@Testcontainers`/`@Container` 미지원)
- `spring.jpa.hibernate.ddl-auto=create-drop`으로 스키마 자동 생성

### DB 초기화 (Test Isolation)

`DatabaseCleanup.java`의 H2 전용 SQL을 PostgreSQL 호환으로 변경:
- **H2**: `SET REFERENTIAL_INTEGRITY FALSE` → `TRUNCATE` → `SET REFERENTIAL_INTEGRITY TRUE`
- **PostgreSQL**: `TRUNCATE wish, option, product, member, category CASCADE`

### 테스트 설계 원칙: "어떻게 되는가"를 검증한다

인수 테스트는 **내부 구현이 아닌 사용자 입력과 그 결과**에 의존해야 한다. 세부 구현(엔티티 구조, repository 메서드, 서비스 내부 로직)에 의존하는 테스트는 리팩토링 시 깨진다. "어떻게 하는가"가 아니라 **"어떻게 되는가"**를 검증하는 테스트를 작성한다.

예시: 재고 차감 검증
- **나쁨** (구현 의존): 선물 후 `optionRepository.findById()`로 quantity 직접 조회
- **좋음** (행위 검증): 재고 전부 소진하는 선물 → 성공 / 같은 옵션에 추가 선물 → 재고 부족으로 실패

### Cucumber 디렉토리 구조

```
src/test/
├── java/gift/
│   ├── cucumber/
│   │   ├── CucumberTest.java              # @Suite + @Tag("cucumber") 엔트리포인트
│   │   ├── CucumberSpringConfig.java      # @CucumberContextConfiguration + Testcontainers
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
    ├── application-cucumber.properties    # Cucumber 프로파일 설정
    ├── cleanup.sql                        # 1단계 H2 테스트용
    └── test-data.sql                      # 1단계 H2 테스트용
```

### Gherkin 작성 원칙
- **비즈니스 언어로 작성**: 기획자/QA가 읽고 이해할 수 있어야 한다
- **구현 세부사항 노출 금지**: HTTP 메서드, 상태 코드, JSON 필드명 등을 시나리오에 직접 쓰지 않는다
- **한국어 Gherkin 키워드 사용**: `기능`, `시나리오`, `Given`/`When`/`Then` (또는 `주어진`/`만일`/`그러면`)

```gherkin
# 좋음 (비즈니스 언어)
시나리오: 재고가 충분하면 선물하기에 성공한다
  주어진 회원 "철수"와 "영희"가 등록되어 있다
  그리고 "생일 케이크" 상품에 "기본 옵션" 재고가 5개 있다
  만일 "철수"가 "영희"에게 "생일 케이크"의 "기본 옵션" 1개를 선물한다
  그러면 선물하기가 성공한다

# 나쁨 (구현 노출)
시나리오: 선물하기 API 호출
  Given POST /api/gifts 요청을 보낸다
  Then 응답 코드가 200이다
```

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
7. 재고 부족 시 선물 실패 — 재고 초과 수량 요청 시 400 응답 (`GlobalExceptionHandler`가 `IllegalStateException`/`NoSuchElementException`을 `BAD_REQUEST`로 처리)

> **WishService**: 컨트롤러가 없으므로 API 레벨 인수 테스트 범위에서 제외.

### 기존 테스트 헬퍼 (1단계에서 작성)
- **`AcceptanceTestSupport`** — 공통 API 호출 헬퍼 클래스 (`카테고리를_생성한다()` 등)
- 각 테스트 클래스 내 private 헬퍼 메서드 — `상품을_조회한다()` 등
- Cucumber step definition에서도 이 헬퍼들을 재사용할 수 있다

## 아키텍처

선물하기 플랫폼 (카카오 선물하기 스타일). Spring Boot 3.5, JPA + PostgreSQL (테스트: Testcontainers), Thymeleaf.

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
