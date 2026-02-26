# Cucumber BDD 적용 가이드

## 1. 핵심 개념

### 1-1. Gherkin의 Given/When/Then

Gherkin은 비기술적 이해관계자도 읽을 수 있는 시나리오 명세 언어다. 세 가지 키워드로 시나리오를 구조화한다.

| 키워드 | 역할 | 이 프로젝트에서의 의미 |
|--------|------|----------------------|
| **Given** (전제) | 시나리오 실행 전 시스템의 초기 상태를 설정한다 | SQL로 테스트 데이터를 준비하고 RestAssured 포트를 설정한다 |
| **When** (행위) | 사용자가 수행하는 핵심 행동을 기술한다 | RestAssured로 API 엔드포인트에 HTTP 요청을 보낸다 |
| **Then** (결과) | 행동의 결과로 기대하는 시스템 상태를 검증한다 | HTTP 상태 코드와 DB 상태를 확인한다 |
| **And** | 같은 단계를 이어서 기술한다 | Given/When/Then 내에서 추가 조건이나 검증을 연결한다 |

현재 프로젝트의 `GiftApiTest.선물_보내기_성공()`을 Gherkin으로 표현하면 다음과 같다.

```gherkin
# 기능: 선물 보내기
# language: ko

기능: 선물 보내기
  사용자가 다른 회원에게 선물을 보낼 수 있다.

  시나리오: 재고가 충분할 때 선물 보내기 성공
    전제 재고가 10인 옵션이 존재한다
    만일 회원 1이 옵션 1을 3개 선물한다
    그러면 응답 상태 코드가 200이다
    그리고 옵션 1의 재고가 7이다
```

### 1-2. Step Definitions에서 파라미터 추출

Gherkin 시나리오의 각 줄은 Java 메서드와 매핑된다. 중괄호 `{type}`으로 파라미터를 추출한다.

```java
import io.cucumber.java.ko.전제;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;

public class GiftStepDefinitions {

    @전제("재고가 {int}인 옵션이 존재한다")
    public void 재고가_N인_옵션이_존재한다(int quantity) {
        // quantity = 10
    }

    @만일("회원 {long}이 옵션 {long}을 {int}개 선물한다")
    public void 회원이_옵션을_N개_선물한다(long memberId, long optionId, int quantity) {
        // memberId = 1, optionId = 1, quantity = 3
    }

    @그러면("응답 상태 코드가 {int}이다")
    public void 응답_상태_코드가_N이다(int statusCode) {
        // statusCode = 200
    }

    @그리고("옵션 {long}의 재고가 {int}이다")
    public void 옵션의_재고가_N이다(long optionId, int expectedQuantity) {
        // optionId = 1, expectedQuantity = 7
    }
}
```

**지원되는 파라미터 타입:**

| 타입 | Gherkin 예시 | 추출 결과 |
|------|-------------|-----------|
| `{int}` | `재고가 10인` | `10` |
| `{long}` | `회원 1이` | `1L` |
| `{string}` | `"생일 축하해"` | `"생일 축하해"` |
| `{float}` | `가격이 99.9인` | `99.9f` |

### 1-3. 시나리오 간 Response 객체 공유

Cucumber에서 하나의 시나리오 내에서 When(요청)과 Then(검증) 단계는 서로 다른 메서드지만, 같은 Response 객체를 참조해야 한다. `@ScenarioScope` Bean으로 시나리오 단위 상태를 공유한다.

```java
import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class ScenarioContext {
    private Response response;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }
}
```

- `@ScenarioScope`는 **시나리오가 시작될 때 Bean을 생성하고 시나리오가 끝나면 폐기**한다.
- 시나리오 간 상태가 격리되므로 테스트 독립성이 보장된다.
- Step Definition 클래스에서 `@Autowired`로 주입받아 사용한다.

```java
public class GiftStepDefinitions {
    @Autowired
    private ScenarioContext context;

    @만일("회원 {long}이 옵션 {long}을 {int}개 선물한다")
    public void 회원이_옵션을_N개_선물한다(long memberId, long optionId, int quantity) {
        Response response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Member-Id", memberId)
            .body("""
                {"optionId": %d, "quantity": %d, "receiverId": 2, "message": "선물"}
                """.formatted(optionId, quantity))
        .when()
            .post("/api/gifts");

        context.setResponse(response);  // 저장
    }

    @그러면("응답 상태 코드가 {int}이다")
    public void 응답_상태_코드가_N이다(int statusCode) {
        context.getResponse()            // 꺼내서 검증
            .then()
            .statusCode(statusCode);
    }
}
```

### 1-4. @Before hook은 언제 실행되는가

`io.cucumber.java.Before`는 **각 시나리오가 시작되기 직전**에 실행된다. JUnit의 `@BeforeEach`에 해당한다.

```java
import io.cucumber.java.Before;

public class Hooks {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    int port;

    @Before
    public void setUp() {
        RestAssured.port = port;  // 매 시나리오 전 포트 설정
    }
}
```

**실행 순서:**

```
시나리오 1 시작
  → @Before (Hooks)
  → Given → When → Then
  → @After (Hooks)
시나리오 2 시작
  → @Before (Hooks)
  → Given → When → Then
  → @After (Hooks)
```

**주의:** `io.cucumber.java.Before`와 `org.junit.jupiter.api.BeforeEach`는 다른 어노테이션이다. Cucumber 테스트에서는 반드시 Cucumber의 `@Before`를 사용해야 한다.

### 1-5. RestAssured 포트 설정 위치

현재 프로젝트는 `@SpringBootTest(RANDOM_PORT)`로 랜덤 포트를 사용한다. Cucumber-Spring 통합 환경에서는 `@CucumberContextConfiguration`이 붙은 클래스에서 포트를 주입받고, `@Before` hook에서 RestAssured에 설정한다.

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {
    // Spring Boot 테스트 컨텍스트를 Cucumber에 연결하는 역할
}
```

```java
public class Hooks {
    @LocalServerPort
    int port;

    @Before
    public void setUp() {
        RestAssured.port = port;
    }
}
```

`@LocalServerPort`는 Spring이 관리하는 Bean이므로, `@CucumberContextConfiguration`으로 Spring 컨텍스트가 초기화된 후에 주입된다.

---

## 2. 프로젝트 적용 방안

### 2-1. 의존성 추가

`build.gradle`에 다음 의존성을 추가한다.

```groovy
dependencies {
    // 기존 의존성 유지
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.rest-assured:rest-assured'

    // Cucumber 추가
    testImplementation 'io.cucumber:cucumber-java:7.22.1'
    testImplementation 'io.cucumber:cucumber-spring:7.22.1'
    testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.22.1'
    testImplementation 'org.junit.platform:junit-platform-suite'
}
```

| 의존성 | 역할 |
|--------|------|
| `cucumber-java` | Step Definition 어노테이션(`@Given`, `@When`, `@Then`, 한글 포함) |
| `cucumber-spring` | `@CucumberContextConfiguration`, `@ScenarioScope` 등 Spring 통합 |
| `cucumber-junit-platform-engine` | JUnit Platform 위에서 Cucumber를 실행하는 엔진 |
| `junit-platform-suite` | `@Suite` 어노테이션으로 Cucumber 테스트를 묶어 실행 |

### 2-2. 디렉토리 구조

```
src/test/
├── java/gift/
│   ├── CucumberTest.java                  # JUnit Platform Suite 러너
│   ├── CucumberSpringConfiguration.java   # Spring Boot 연동 설정
│   ├── ScenarioContext.java               # 시나리오별 상태 공유 (Response 등)
│   ├── Hooks.java                         # @Before/@After (포트 설정, 데이터 초기화)
│   └── stepdefs/                          # Step Definitions
│       ├── GiftStepDefinitions.java
│       ├── ProductStepDefinitions.java
│       ├── WishStepDefinitions.java
│       └── CategoryStepDefinitions.java
├── resources/
│   ├── features/                          # Gherkin Feature 파일
│   │   ├── gift.feature
│   │   ├── product.feature
│   │   ├── wish.feature
│   │   └── category.feature
│   └── sql/                               # 기존 SQL 파일 (그대로 유지)
│       ├── common-init.sql
│       ├── gift/
│       ├── option/
│       └── wish/
```

### 2-3. 설정 클래스 구현

#### CucumberTest.java — 테스트 러너

```java
package gift;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectPackages("gift")
public class CucumberTest {
}
```

`@Suite`와 `@IncludeEngines("cucumber")`의 조합으로 JUnit Platform이 Cucumber 엔진을 통해 Feature 파일을 탐색하고 실행한다.

#### CucumberSpringConfiguration.java — Spring 연동

```java
package gift;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {
}
```

- `@CucumberContextConfiguration`은 **Cucumber가 사용할 Spring ApplicationContext를 지정**한다.
- 기존 테스트와 동일하게 `RANDOM_PORT`로 실제 서버를 구동한다.
- 이 클래스는 하나만 존재해야 한다.

#### ScenarioContext.java — 시나리오 상태 공유

```java
package gift;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class ScenarioContext {
    private Response response;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }
}
```

#### Hooks.java — 공통 전/후 처리

```java
package gift;

import io.cucumber.java.Before;
import io.restassured.RestAssured;
import org.springframework.boot.test.web.server.LocalServerPort;

public class Hooks {

    @LocalServerPort
    int port;

    @Before
    public void setUp() {
        RestAssured.port = port;
    }
}
```

기존 테스트의 `@BeforeEach`가 하던 포트 설정을 Cucumber의 `@Before` hook으로 대체한다.

### 2-4. Feature 파일 작성 — 기존 테스트의 전환

현재 14개 테스트를 4개 Feature 파일로 전환한다.

#### features/gift.feature

```gherkin
# language: ko

기능: 선물 보내기
  사용자가 다른 회원에게 상품 옵션을 선물할 수 있다.
  선물 시 해당 옵션의 재고가 자동으로 차감된다.

  시나리오: 재고가 충분할 때 선물 보내기 성공
    전제 재고가 10인 옵션이 존재한다
    만일 회원 1이 옵션 1을 3개 선물한다
    그러면 응답 상태 코드가 200이다
    그리고 옵션 1의 재고가 7이다

  시나리오: 재고와 요청 수량이 같으면 재고가 0이 된다
    전제 재고가 5인 옵션이 존재한다
    만일 회원 1이 옵션 1을 5개 선물한다
    그러면 응답 상태 코드가 200이다
    그리고 옵션 1의 재고가 0이다

  시나리오: 재고 부족 시 실패한다
    전제 재고가 2인 옵션이 존재한다
    만일 회원 1이 옵션 1을 5개 선물한다
    그러면 응답 상태 코드가 500이다
    그리고 옵션 1의 재고가 2이다

  시나리오: 재고가 0일 때 실패한다
    전제 재고가 0인 옵션이 존재한다
    만일 회원 1이 옵션 1을 1개 선물한다
    그러면 응답 상태 코드가 500이다
    그리고 옵션 1의 재고가 0이다

  시나리오: 존재하지 않는 옵션으로 선물 보내기 시 실패한다
    전제 공통 데이터가 초기화되어 있다
    만일 회원 1이 옵션 999을 1개 선물한다
    그러면 응답 상태 코드가 500이다

  시나리오: 옵션 ID에 잘못된 타입을 보내면 실패한다
    전제 공통 데이터가 초기화되어 있다
    만일 옵션 ID에 "잘못된값"을 담아 선물 요청을 보낸다
    그러면 응답 상태 코드가 400이다
```

#### features/product.feature

```gherkin
# language: ko

기능: 상품 등록
  관리자가 카테고리에 속하는 상품을 등록할 수 있다.

  시나리오: 상품 등록 성공
    전제 공통 데이터가 초기화되어 있다
    만일 이름이 "초콜릿"이고 가격이 10000이고 카테고리가 1인 상품을 등록한다
    그러면 응답 상태 코드가 200이다
    그리고 상품이 1개 저장되어 있다
    그리고 상품 "초콜릿"의 카테고리 ID가 1이다

  시나리오: 가격에 잘못된 타입을 보내면 실패한다
    전제 공통 데이터가 초기화되어 있다
    만일 가격에 "만원"을 담아 상품 등록 요청을 보낸다
    그러면 응답 상태 코드가 400이다
    그리고 상품이 0개 저장되어 있다

  시나리오: 존재하지 않는 카테고리로 상품 등록 시 실패한다
    전제 공통 데이터가 초기화되어 있다
    만일 이름이 "초콜릿"이고 가격이 10000이고 카테고리가 999인 상품을 등록한다
    그러면 응답 상태 코드가 500이다
    그리고 상품이 0개 저장되어 있다
```

#### features/wish.feature

```gherkin
# language: ko

기능: 위시리스트에 상품 추가
  회원이 관심 있는 상품을 위시리스트에 추가할 수 있다.

  시나리오: 위시리스트 추가 성공
    전제 상품 1이 존재한다
    만일 회원 1이 상품 1을 위시리스트에 추가한다
    그러면 응답 상태 코드가 200이다
    그리고 위시리스트에 1개 항목이 저장되어 있다
    그리고 위시리스트에 회원 1과 상품 1의 연결이 존재한다

  시나리오: 존재하지 않는 상품에 위시리스트 추가 시 실패한다
    전제 공통 데이터가 초기화되어 있다
    만일 회원 1이 상품 999을 위시리스트에 추가한다
    그러면 응답 상태 코드가 500이다
    그리고 위시리스트에 0개 항목이 저장되어 있다

  시나리오: 상품 ID에 잘못된 타입을 보내면 실패한다
    전제 공통 데이터가 초기화되어 있다
    만일 상품 ID에 "잘못된값"을 담아 위시리스트 요청을 보낸다
    그러면 응답 상태 코드가 400이다
    그리고 위시리스트에 0개 항목이 저장되어 있다

  시나리오: 존재하지 않는 회원이 위시리스트 추가 시 실패한다
    전제 상품 1이 존재한다
    만일 회원 999이 상품 1을 위시리스트에 추가한다
    그러면 응답 상태 코드가 500이다
    그리고 위시리스트에 0개 항목이 저장되어 있다
```

#### features/category.feature

```gherkin
# language: ko

기능: 카테고리 생성
  관리자가 상품 분류를 위한 카테고리를 생성할 수 있다.

  시나리오: 카테고리 생성 성공
    전제 공통 데이터가 초기화되어 있다
    만일 이름이 "뷰티"인 카테고리를 생성한다
    그러면 응답 상태 코드가 200이다
    그리고 카테고리 "뷰티"가 저장되어 있다
```

### 2-5. Step Definitions 구현 — 기존 테스트 로직 매핑

기존 `@Sql` 기반 데이터 초기화를 Step Definition의 Given 단계로 이전한다. `JdbcTemplate`으로 SQL 파일을 실행한다.

#### GiftStepDefinitions.java

```java
package gift.stepdefs;

import gift.ScenarioContext;
import gift.model.Option;
import gift.model.OptionRepository;
import io.cucumber.java.ko.그러면;
import io.cucumber.java.ko.그리고;
import io.cucumber.java.ko.만일;
import io.cucumber.java.ko.전제;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

public class GiftStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private DataSource dataSource;

    @전제("공통 데이터가 초기화되어 있다")
    public void 공통_데이터가_초기화되어_있다() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/common-init.sql"));
        }
    }

    @전제("재고가 {int}인 옵션이 존재한다")
    public void 재고가_N인_옵션이_존재한다(int quantity) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/common-init.sql"));
            // quantity에 따라 적절한 시나리오 SQL 실행
            String sqlFile = switch (quantity) {
                case 10 -> "sql/gift/success.sql";
                case 5 -> "sql/gift/exact-quantity.sql";
                case 2 -> "sql/gift/insufficient-stock.sql";
                case 0 -> "sql/gift/zero-stock.sql";
                default -> throw new IllegalArgumentException("미지원 재고 수량: " + quantity);
            };
            ScriptUtils.executeSqlScript(conn, new ClassPathResource(sqlFile));
        }
    }

    @만일("회원 {long}이 옵션 {long}을 {int}개 선물한다")
    public void 회원이_옵션을_N개_선물한다(long memberId, long optionId, int quantity) {
        context.setResponse(
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", memberId)
                .body("""
                    {"optionId": %d, "quantity": %d, "receiverId": 2, "message": "선물"}
                    """.formatted(optionId, quantity))
            .when()
                .post("/api/gifts")
        );
    }

    @만일("옵션 ID에 {string}을 담아 선물 요청을 보낸다")
    public void 옵션_ID에_잘못된값을_담아_선물_요청을_보낸다(String invalidValue) {
        context.setResponse(
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Member-Id", 1L)
                .body("""
                    {"optionId": "%s", "quantity": 1, "receiverId": 2, "message": "선물"}
                    """.formatted(invalidValue))
            .when()
                .post("/api/gifts")
        );
    }

    @그리고("옵션 {long}의 재고가 {int}이다")
    public void 옵션의_재고가_N이다(long optionId, int expectedQuantity) {
        Option option = optionRepository.findById(optionId).orElseThrow();
        assertThat(option.getQuantity()).isEqualTo(expectedQuantity);
    }
}
```

#### 공통 Step (응답 검증)

```java
package gift.stepdefs;

import gift.ScenarioContext;
import io.cucumber.java.ko.그러면;
import org.springframework.beans.factory.annotation.Autowired;

public class CommonStepDefinitions {

    @Autowired
    private ScenarioContext context;

    @그러면("응답 상태 코드가 {int}이다")
    public void 응답_상태_코드가_N이다(int statusCode) {
        context.getResponse()
            .then()
            .statusCode(statusCode);
    }
}
```

### 2-6. 기존 테스트와의 대응 관계

| 기존 테스트 (JUnit) | Cucumber Feature | Given (데이터 준비) | When (행동) | Then (검증) |
|---------------------|-----------------|---------------------|-------------|-------------|
| `GiftApiTest.선물_보내기_성공` | gift.feature 시나리오 1 | `@Sql(common-init, gift/success)` → `재고가 10인 옵션이 존재한다` | `.post("/api/gifts")` → `회원 1이 옵션 1을 3개 선물한다` | `.statusCode(200)` + `재고 == 7` → `응답 상태 코드가 200이다` + `옵션 1의 재고가 7이다` |
| `ProductApiTest.상품_등록_성공` | product.feature 시나리오 1 | `@Sql(common-init)` → `공통 데이터가 초기화되어 있다` | `.post("/api/products")` → `이름이 "초콜릿"이고...` | `.statusCode(200)` + `상품 1개` → `응답 상태 코드가 200이다` + `상품이 1개 저장되어 있다` |
| `WishApiTest.위시리스트_추가_성공` | wish.feature 시나리오 1 | `@Sql(common-init, wish/success)` → `상품 1이 존재한다` | `.post("/api/wishes")` → `회원 1이 상품 1을 위시리스트에 추가한다` | `.statusCode(200)` + `wish 1개` → `응답 상태 코드가 200이다` + `위시리스트에 1개 항목이 저장되어 있다` |

### 2-7. 데이터 초기화 전략 비교

| 항목 | 현재 (`@Sql`) | Cucumber (`@Before` + `ScriptUtils`) |
|------|--------------|--------------------------------------|
| 실행 시점 | 각 `@Test` 메서드 직전 | 각 시나리오의 Given 단계 |
| 선언 위치 | 테스트 메서드 위 `@Sql` 어노테이션 | Step Definition 메서드 내 |
| SQL 파일 | 동일한 파일 재사용 | 동일한 파일 재사용 |
| 트랜잭션 | RestAssured 별도 스레드이므로 롤백 불가 | 동일 (RestAssured 사용) |

기존 SQL 파일(`common-init.sql`, `gift/success.sql` 등)은 변경 없이 그대로 사용한다.

### 2-8. JUnit Platform 설정

`src/test/resources/junit-platform.properties` 파일을 생성하여 Cucumber 옵션을 설정한다.

```properties
cucumber.glue=gift
cucumber.features=src/test/resources/features
cucumber.plugin=pretty
```

| 속성 | 설명 |
|------|------|
| `cucumber.glue` | Step Definitions를 탐색할 패키지 |
| `cucumber.features` | Feature 파일 위치 |
| `cucumber.plugin` | 출력 형식 (pretty: 컬러 콘솔 출력) |

---

## 3. 핵심 개념 요약

| 개념 | 설명 | 이 프로젝트에서의 역할 |
|------|------|----------------------|
| `@CucumberContextConfiguration` | Cucumber가 사용할 Spring 컨텍스트를 지정 | `@SpringBootTest(RANDOM_PORT)`와 결합하여 서버 구동 |
| `@ScenarioScope` | 시나리오 단위로 Bean 생명주기를 관리 | `ScenarioContext`에서 Response 객체를 시나리오 내 공유 |
| `io.cucumber.java.ko` | 한글 Gherkin 키워드에 대응하는 Step Definition 어노테이션 | `@전제`, `@만일`, `@그러면`, `@그리고`로 한글 시나리오 매핑 |
| Feature file | Gherkin 문법으로 작성된 시나리오 명세 | `src/test/resources/features/*.feature` |
| `@Suite` + `@IncludeEngines("cucumber")` | JUnit Platform 위에서 Cucumber 엔진 실행 | `CucumberTest.java`에서 전체 Feature 실행 |
| `@Before` (Cucumber) | 각 시나리오 시작 전 실행되는 hook | RestAssured 포트 설정, 데이터 초기화 |
