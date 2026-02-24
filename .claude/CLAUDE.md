# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 빌드 및 테스트 명령어

```bash
./gradlew build          # 빌드 + 테스트
./gradlew test           # 전체 테스트 실행
./gradlew bootRun        # 애플리케이션 실행 (H2 인메모리 DB, 포트 8080)
./gradlew test --tests "gift.SomeTest"              # 단일 테스트 클래스 실행
./gradlew test --tests "gift.SomeTest.methodName"   # 단일 테스트 메서드 실행
```

Java 21 필수.

## 현재 과제: 2단계 — Cucumber/Gherkin 기반 인수 테스트

1단계에서 작성한 인수 테스트를 **Cucumber/Gherkin** 기반으로 전환한다. 기획자/QA와 소통 가능한 **비즈니스 언어 시나리오**를 작성하는 것이 목표이다.

### 제출물
1. **Gherkin 시나리오 (.feature 파일)** — 비즈니스 언어로 작성된 인수 테스트 시나리오
2. **Step Definition 코드** — Gherkin 시나리오를 실행하는 Java 코드
3. **AI 활용 문서** — 프롬프트 및 접근 방법 정리

### 제약 조건
- **Cucumber + Gherkin** 사용 필수
- 사용자 관점에서 행위를 검증하는 테스트 (API 레벨의 인수 테스트)
- Gherkin 시나리오는 **기획자/QA가 읽을 수 있는 비즈니스 언어**로 작성 (구현 세부사항 노출 금지)

### 테스트 설계 원칙: "어떻게 되는가"를 검증한다

인수 테스트는 **내부 구현이 아닌 사용자 입력과 그 결과**에 의존해야 한다. 세부 구현(엔티티 구조, repository 메서드, 서비스 내부 로직)에 의존하는 테스트는 리팩토링 시 깨진다. "어떻게 하는가"가 아니라 **"어떻게 되는가"**를 검증하는 테스트를 작성한다.

예시: 재고 차감 검증
- **나쁨** (구현 의존): 선물 후 `optionRepository.findById()`로 quantity 직접 조회
- **좋음** (행위 검증): 재고 전부 소진하는 선물 → 성공 / 같은 옵션에 추가 선물 → 재고 부족으로 실패

### 테스트 작성 가이드

#### 기술 스택
- `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` + **RestAssured** + **Cucumber**
- H2 인메모리 DB 사용 (별도 설정 불필요)
- Gradle 의존성:

```groovy
testImplementation 'io.rest-assured:rest-assured'
testImplementation 'io.cucumber:cucumber-java:7.22.1'
testImplementation 'io.cucumber:cucumber-spring:7.22.1'
testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.22.1'
testImplementation 'org.junit.platform:junit-platform-suite'
```

#### Cucumber 디렉토리 구조

```
src/test/
├── java/gift/
│   ├── cucumber/
│   │   ├── CucumberTest.java              # @Suite 엔트리포인트
│   │   ├── CucumberSpringConfig.java      # @CucumberContextConfiguration + @SpringBootTest
│   │   └── steps/
│   │       ├── CategorySteps.java         # 카테고리 관련 step definitions
│   │       ├── ProductSteps.java          # 상품 관련 step definitions
│   │       └── GiftSteps.java             # 선물하기 관련 step definitions
│   └── AcceptanceTestSupport.java         # 공통 API 호출 헬퍼 (1단계에서 작성)
└── resources/
    ├── features/
    │   ├── category.feature
    │   ├── product.feature
    │   └── gift.feature
    ├── cleanup.sql
    └── test-data.sql
```

#### Cucumber + Spring 통합

```java
// CucumberSpringConfig.java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfig {
    // Spring 컨텍스트 설정
}

// CucumberTest.java — Cucumber 테스트 실행 엔트리포인트
@Suite
@IncludeEngines("cucumber")
@SelectPackages("gift.cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "gift.cucumber")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/resources/features")
public class CucumberTest {}
```

#### Gherkin 작성 원칙
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

#### 컨트롤러 요청 바인딩
- 모든 POST 엔드포인트(`/api/products`, `/api/categories`, `/api/gifts`)에 `@RequestBody`가 있음 → **JSON body**로 전송

```java
// RestAssured 사용 예시
ExtractableResponse<Response> response = RestAssured.given().log().all()
        .contentType(ContentType.JSON)
        .header("Accept", "application/json")
        .header("Member-Id", 1)
        .body(createGiftRequest(1L, 1, 2L, "생일 축하"))
        .when().post("/api/gifts")
        .then().log().all().extract();
```

#### 테스트 데이터 전략: @Sql 스크립트
- repository를 직접 사용하면 Java 엔티티/생성자에 의존 → 리팩토링 시 깨짐
- **@Sql 스크립트**로 데이터를 준비하고 정리한다 (구현 비의존)
- `src/test/resources/`에 SQL 파일 배치

```java
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
```

- cleanup.sql → `SET REFERENTIAL_INTEGRITY FALSE` 후 모든 테이블 TRUNCATE → PK 시퀀스도 초기화
- test-data.sql → 테스트에 필요한 기본 데이터 INSERT
- H2의 컬럼명은 JPA 네이밍 전략을 따름 (예: `imageUrl` → `image_url`)
- Cucumber에서는 `@Sql` 대신 Step Definition 내에서 JDBC 또는 API 호출로 데이터를 준비/정리할 수 있다

#### 테스트 격리
- **`RANDOM_PORT`에서 `@Transactional` 롤백은 동작하지 않는다.** 실제 HTTP 요청은 별도 스레드에서 처리되므로 테스트 트랜잭션과 분리됨.
- 매 테스트 전 `@Sql`로 cleanup → 데이터 재세팅하여 격리 보장

#### 검증 전략: 다음 행동으로 이전 행동을 검증
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

> **WishService**: 컨트롤러가 없으므로 API 레벨 인수 테스트 범위에서 제외. 필요 시 서비스 레벨 테스트로 별도 분리 가능하나, "사용자 관점 행위 검증" 취지와 맞지 않음.

### 기존 테스트 헬퍼 (1단계에서 작성)
- **`AcceptanceTestSupport`** — 공통 API 호출 헬퍼 클래스 (`카테고리를_생성한다()` 등)
- 각 테스트 클래스 내 private 헬퍼 메서드 — `상품을_조회한다()` 등
- Cucumber step definition에서도 이 헬퍼들을 재사용할 수 있다

## 아키텍처

선물하기 플랫폼 (카카오 선물하기 스타일). Spring Boot 3.5, JPA + H2, Thymeleaf.

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
