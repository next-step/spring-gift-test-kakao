---
name: cucumber-converter
description: >
  기존 RestAssured E2E 테스트를 Cucumber + Gherkin 문법으로 변환한다.
  대상 테스트 클래스를 인자로 받거나, 미지정 시 전체 AcceptanceTest를 순서대로 변환한다.
  .feature 파일과 Step Definition 클래스를 생성하며, 기존 테스트 동작을 100% 보존한다.
argument-hint: "[테스트 클래스명, e.g. CategoryAcceptanceTest]"
allowed-tools: Read, Grep, Glob, Edit, Write, Bash(./gradlew *)
---

# cucumber-converter

기존 RestAssured 기반 E2E 테스트를 Cucumber + Gherkin 문법으로 변환한다.

## 역할
- 기존 `*AcceptanceTest.java`의 테스트 시나리오를 `.feature` 파일로 추출한다.
- RestAssured 호출 로직을 Step Definition 클래스로 이전한다.
- 변환 전후 테스트 결과가 동일함을 보장한다.

## 입력 계약
- 사용자가 대상 테스트 클래스(예: `CategoryAcceptanceTest`)를 지정하면 해당 클래스만 변환한다.
- 대상이 없으면 전체 `*AcceptanceTest.java`를 순서대로 변환한다.
  - 순서: Category → Product → Gift
- 변환 전 반드시 기존 테스트 코드와 프로덕션 코드를 읽어 동작을 파악한다.

## 조건 조건 확인

변환 시작 전 아래를 확인하고, 누락 시 먼저 조치한다:

### 1. Cucumber 의존성 (`build.gradle`)
```groovy
testImplementation 'io.cucumber:cucumber-java:7.14.0'
testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.14.0'
testImplementation 'io.cucumber:cucumber-spring:7.14.0'
```

### 2. JUnit Platform 설정 (`src/test/resources/junit-platform.properties`)
```properties
cucumber.plugin=pretty,html:build/reports/cucumber.html
cucumber.glue=gift
cucumber.features=src/test/resources/features
cucumber.publish.quiet=true
```

### 3. Feature 파일 디렉토리
```
src/test/resources/features/
```

## 변환 결과물 구조

```
src/test/
├── java/gift/
│   ├── CucumberTest.java              ← Cucumber 실행 진입점
│   ├── CucumberSpringConfig.java      ← Spring 컨텍스트 설정
│   ├── steps/
│   │   ├── CategorySteps.java         ← Category 시나리오 Step Definitions
│   │   ├── ProductSteps.java          ← Product 시나리오 Step Definitions
│   │   ├── GiftSteps.java             ← Gift 시나리오 Step Definitions
│   │   └── CommonSteps.java           ← 공통 Given/Then (데이터 정리, 상태 코드 검증 등)
│   └── (기존 *AcceptanceTest.java는 삭제하지 않음 — 사용자 판단에 위임)
└── resources/
    ├── features/
    │   ├── category.feature
    │   ├── product.feature
    │   └── gift.feature
    └── junit-platform.properties
```

## Gherkin 작성 규칙

### Feature 파일 언어: 한글 (`# language: ko`)
```gherkin
# language: ko
기능: 카테고리 API

  시나리오: 카테고리 생성 성공
    만일 이름이 "전자기기"인 카테고리를 생성하면
    그러면 응답 상태 코드는 200이다
    그리고 응답의 "id" 필드는 null이 아니다
    그리고 응답의 "name" 필드는 "전자기기"이다

  시나리오: 카테고리 목록 조회 - 빈 목록
    만일 카테고리 목록을 조회하면
    그러면 응답 상태 코드는 200이다
    그리고 응답 목록의 크기는 0이다

  시나리오: 카테고리 목록 조회 - N개 존재
    조건 이름이 "전자기기"인 카테고리가 등록되어 있다
    그리고 이름이 "식품"인 카테고리가 등록되어 있다
    만일 카테고리 목록을 조회하면
    그러면 응답 상태 코드는 200이다
    그리고 응답 목록의 크기는 2이다
    그리고 응답 목록의 "name"에 "전자기기", "식품"이 순서 무관하게 포함되어 있다
```

### 한글 키워드 매핑
| Gherkin (영어) | 한글 키워드 |
|----------------|------------|
| Feature | 기능 |
| Background | 배경 |
| Scenario | 시나리오 |
| Given | 조건 |
| When | 만일 |
| Then | 그러면 |
| And | 그리고 |
| But | 하지만 |

### 시나리오 작성 원칙
- **한 시나리오 = 한 테스트 메서드**: 기존 `@Test` 메서드와 1:1 매핑
- **Step 재사용 극대화**: 공통 Given/Then은 `CommonSteps`에 배치
- **구체적인 값 사용**: `<placeholder>` 대신 실제 값으로 작성 (Scenario Outline은 필요 시만)
- **비즈니스 언어 사용**: 기술 용어(statusCode, JSON 등) 대신 도메인 용어

## Step Definition 작성 규칙

### Spring 컨텍스트 설정
```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfig {
}
```

### Cucumber 실행 진입점
```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
public class CucumberTest {
}
```

### Step 클래스 구조
```java
public class CategorySteps {

    @Autowired
    CategoryRepository categoryRepository;

    // RestAssured 응답을 Step 간 공유
    private Response response;

    @Given("이름이 {string}인 카테고리가 등록되어 있다")
    public void 카테고리_등록(String name) {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(200);
    }

    @When("이름이 {string}인 카테고리를 생성하면")
    public void 카테고리_생성(String name) {
        response = given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/categories");
    }
}
```

### 상태 공유 규칙
- **Response 객체**: 각 도메인 Steps 클래스 내에서 인스턴스 변수로 관리
- **Cross-step 데이터** (ID 등): Steps 클래스 내 인스턴스 변수로 저장
- **RestAssured port**: `CommonSteps`의 `@Before`에서 설정

### 데이터 생성 전략 (기존 규칙 유지)
- API가 있는 엔티티 (Category, Product) → API 호출로 생성 (Given step)
- API가 없는 엔티티 (Member, Option) → Repository로 생성 (Given step)

### CommonSteps 구조
```java
public class CommonSteps {

    @LocalServerPort
    int port;

    @Autowired OptionRepository optionRepository;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired MemberRepository memberRepository;

    @Before
    public void setUp() {
        RestAssured.port = port;
        // 매 시나리오 전 자동 cleanup — FK 역순 삭제
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }
}
```

### 데이터 격리 보장 방식
- Cucumber `@Before` hook은 **매 시나리오(Scenario) 실행 전** 자동으로 호출된다.
- feature 파일에 `배경(Background)` 블록을 작성할 필요 없이 격리가 보장된다.
- `@Before`는 `io.cucumber.java.Before`를 사용한다 (JUnit의 `@BeforeEach`가 아님).

## 실행 흐름

```
1. 대상 테스트 클래스 확인 (사용자 지정 또는 전체)
2. 조건 조건 확인 (build.gradle, junit-platform.properties)
3. 기존 테스트 코드 읽기 — 시나리오/검증 포인트 파악
4. .feature 파일 작성 (src/test/resources/features/)
5. Step Definition 클래스 작성 (src/test/java/gift/steps/)
6. CucumberSpringConfig.java, CucumberTest.java 생성
7. ./gradlew test 실행
8. 실패 시 Step Definition 수정 (feature 파일 우선 보존)
9. 전체 통과 확인
```

## 테스트 실패 대응

1. **Step 매칭 실패**: Step Definition의 정규식/파라미터 수정
2. **Spring 컨텍스트 문제**: `CucumberSpringConfig` 설정 확인
3. **데이터 격리 실패**: `배경` 스텝의 cleanup 순서 확인
4. **프로덕션 코드 문제**: 기존 테스트와 동일하게 현재 동작 기준으로 작성

## 파일 수정 범위 제약

### 수정 가능 경로
- `src/test/**/*` — 테스트 코드 생성/수정
- `src/test/resources/**/*` — feature 파일, 테스트 설정
- `build.gradle` — 테스트 의존성 추가만 허용 (`testImplementation` 행만)

### 절대 수정 금지 경로
- `src/main/**/*` — 프로덕션 코드 일체

## 금지 사항
- 프로덕션 코드 수정
- 기존 `*AcceptanceTest.java` 삭제 (사용자 판단에 위임)
- 기존 테스트의 검증 항목 누락 (모든 assert를 feature step으로 이전)
- 영어 Gherkin 키워드 사용 (반드시 한글 키워드)
- Step Definition에서 직접 테스트 로직 하드코딩 (파라미터화 필수)

## 사용 예시

```text
/cucumber-converter
```

```text
/cucumber-converter CategoryAcceptanceTest
```

```text
/cucumber-converter Gift API
```
