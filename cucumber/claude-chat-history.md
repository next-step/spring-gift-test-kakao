# Cucumber BDD 도입 진행 과정 (Claude code 사용 내역)

## 1. Cucumber 도입을 위한 사전 조사

### 요청

- 기존 인수 테스트를 Cucumber로 재구성하기 위해 필요한 개념과 준비물 정리

### 결과

- **핵심 개념 정리**: Gherkin 문법, Step Definition, Cucumber Expression, cucumber-spring 통합, Hooks, Scenario Outline, Tags, Background
- **필요 의존성**: `cucumber-java`, `cucumber-spring`, `cucumber-junit-platform-engine`
- **프로젝트 구조**: `.feature` 파일 위치, Step Definition 클래스 구조, Spring 설정 클래스 구조 설계

---

## 2. build.gradle 의존성 추가 및 테스트 실행 설정

### 요청

- build.gradle에 Cucumber 의존성 추가
- `./gradlew test` 실행 시 Cucumber 테스트도 함께 실행되도록 구성

### 결과

- **build.gradle**: Cucumber BOM(7.20.1) 기반으로 3개 의존성 추가
    - `cucumber-java` — Step Definition 어노테이션
    - `cucumber-spring` — Spring Boot 컨텍스트 통합
    - `cucumber-junit-platform-engine` — JUnit Platform 위에서 Cucumber 실행
- **junit-platform.properties 생성**: `src/test/resources/junit-platform.properties`
    - `cucumber.glue=gift` (Step Definition 탐색 패키지)
    - `cucumber.features=classpath:features` (.feature 파일 위치)
    - `cucumber.plugin=pretty` (콘솔 출력 포맷)
- 기존 JUnit 테스트와 공존 확인 (`./gradlew test` BUILD SUCCESSFUL)

---

## 3. 기존 인수 테스트를 Gherkin Feature 파일로 변환

### 요청

- 현재 구성된 테스트(CategoryAcceptanceTest, ProductAcceptanceTest, GiftAcceptanceTest)를 Gherkin `.feature` 파일로 정의

### 결과

- **category.feature** (3 시나리오): 추가, 조회, 추가 후 조회
- **product.feature** (6 시나리오): 추가, 조회, 추가 후 조회, 카테고리 id null/미존재 에러, 음수 가격 에러
- **gift.feature** (8 시나리오): 선물 보내기, 옵션/수신자/Member-Id null/미존재 에러, 재고 부족 에러
- Background를 활용해 공통 사전 데이터 세팅 (기존 `@BeforeEach` 역할)

---

## 4. 테스트 케이스 추가 및 Step 재사용을 위한 리팩토링

### 요청

- 새로운 테스트 케이스 추가:
    - 재고 경계값 테스트 (n-1, n, n+1개 선물 → 성공/실패)
    - 재고 0개일 때 선물 → 실패
    - 카테고리/상품 빈 목록 조회
- Feature 간 Step Definition 재사용이 가능하도록 리팩토링

### 결과

**추가된 시나리오:**

- `category.feature` — "카테고리가 없으면 빈 목록이 조회된다"
- `product.feature` — "상품이 없으면 빈 목록이 조회된다"
- `gift.feature` — Scenario Outline으로 재고 경계값 테스트 (stock=10 → 9/10/11개, stock=0 → 1개)

**Step 재사용 리팩토링:**

- gift.feature의 `카테고리의 상품 "X"가 ...` → `상품 "X"가 ...`로 통일 (product.feature와 동일한 step definition 공유)
- 기존 단독 재고 부족 시나리오 → Scenario Outline로 대체
- 빈 목록 검증에 기존 `응답 목록의 크기는 {int}이다` step 재사용 (크기 0)

**Feature 간 공유 가능한 주요 Step:**

- `카테고리 "{string}"가 존재한다` — category, product, gift
- `상태 코드는 {int}이다` — 전체
- `응답 목록의 크기는 {int}이다` — category, product
- `응답 목록의 "{string}"에 "{string}"가 포함되어 있다` — category, product

---

## 5. Step Definition 클래스 및 Cucumber-Spring 설정 작성

### 요청

- Feature 파일의 step들에 대한 Step Definition Java 클래스 작성
- Cucumber + Spring 통합 설정 (공식 문서 3종 참고)
    - [Sharing state between scenarios](https://cucumber.io/docs/cucumber/state/#sharing-state-between-scenarios)
    - [cucumber-spring README](https://raw.githubusercontent.com/cucumber/cucumber-jvm/refs/heads/main/cucumber-spring/README.md)
    - [How to use DI](https://cucumber.io/docs/cucumber/state/#how-to-use-di)

### 결과

**공식 문서에서 학습한 핵심 패턴:**

- `@CucumberContextConfiguration` + `@SpringBootTest`로 Spring 컨텍스트 통합
- `@ScenarioScope`로 시나리오 단위 상태 격리 (시나리오마다 새 인스턴스 생성)
- 생성자 주입 권장 (공식 문서 추천 방식)
- Cucumber의 glue code 클래스는 자동으로 Spring 빈으로 관리됨

**생성 파일 (패키지: `gift.cucumber`):**

| 파일                             | 역할                                                                                |
|--------------------------------|-----------------------------------------------------------------------------------|
| `CucumberSpringConfig.java`    | `@CucumberContextConfiguration` + `@SpringBootTest` — Cucumber-Spring 통합 진입점      |
| `AcceptanceContext.java`       | `@Component` + `@ScenarioScope` — 시나리오 내 step 간 상태 공유 (Response, 엔티티 ID 맵)        |
| `CucumberHooks.java`           | `@Before`: RestAssured 포트/로깅 설정, `@After`: `TestDataManipulator.initAll()` 데이터 정리 |
| `CommonStepDefinitions.java`   | 공통 Then 검증 step 7개 (상태코드, 응답 필드, 목록 검증)                                           |
| `CategoryStepDefinitions.java` | 카테고리 Given/When step 3개                                                           |
| `ProductStepDefinitions.java`  | 상품 Given/When step 6개                                                             |
| `GiftStepDefinitions.java`     | 회원·옵션 Given + 선물 When step 10개                                                    |

**설계 포인트:**

- `AcceptanceContext`에 이름 기반 엔티티 ID 맵 (`Map<String, Long>`) 사용하여 step 간 데이터 전달
- `lastCategoryId`/`lastProductId` 패턴으로 Background 내 순차적 데이터 설정의 암묵적 연결
- 기존 `TestDataManipulator` 재활용 (Given step에서 JPA를 통한 사전 데이터 구성)
- `Long.MAX_VALUE`로 존재하지 않는 ID 표현 (기존 JUnit 테스트 패턴 유지)

**테스트 실행 결과:** 22 Cucumber 시나리오 전체 통과 + 기존 JUnit 17개 통과 = 39 tests, 0 failures

---

## 6. Step Definition 메서드명 영문화 및 패키지 구조 정리

### 요청

1. Step Definition 메서드명을 한글 → 영문으로 변경
2. 패키지 구조 재정리

### 결과

**메서드명 영문화:**

- `@Given`/`@When`/`@Then` 어노테이션의 Gherkin 표현식은 유지, Java 메서드명만 변경
- 예: `상태_코드_검증()` → `verifyStatusCode()`, `카테고리_존재()` → `categoryExists()`

**패키지 구조 정리:**

| 패키지                     | 파일                                                             | 역할                          |
|-------------------------|----------------------------------------------------------------|-----------------------------|
| `gift.cucumber`         | `CategoryFeatureTest`, `ProductFeatureTest`, `GiftFeatureTest` | Feature별 Suite Runner       |
| `gift.cucumber.config`  | `CucumberSpringConfig`, `CucumberHooks`                        | Cucumber-Spring 설정 및 생명주기 훅 |
| `gift.cucumber.context` | `AcceptanceContext`                                            | 시나리오 내 step 간 상태 공유         |
| `gift.cucumber.step`    | `Common/Category/Product/GiftStepDefinitions`                  | Step Definition             |

---

## 7. Feature별 Suite Runner 도입

### 요청

- IDE/Gradle에서 Cucumber 테스트가 Feature별로 분리되어 표시되도록 구성
- 기존에는 모든 Cucumber 시나리오가 "카테고리 관리" 하나의 그룹에 묶여 표시되는 문제

### 배경 학습

**`junit-platform.properties`의 역할:**

- JUnit Platform이 테스트 실행 시 읽는 전역 설정 파일 (`src/test/resources/`)
- `cucumber.glue` — Step Definition을 탐색할 루트 Java 패키지 지정
- `cucumber.features` — `.feature` 파일 탐색 경로
- `cucumber.plugin` — 콘솔 출력 포맷

**왜 "카테고리 관리"에 모든 테스트가 묶였는가:**

- Gradle은 테스트 결과를 **클래스 > 메서드** 2단계로 표시
- Cucumber 엔진은 **엔진 > Feature > Scenario** 3단계 구조
- Gradle이 3단계를 2단계에 매핑하면서, 알파벳순 첫 번째 Feature인 "카테고리 관리"를 최상위 그룹으로 사용

**`@Suite`의 역할:**

- 여러 테스트를 하나의 그룹으로 묶어 실행하는 JUnit Platform 기능
- `.feature` 파일은 Java 클래스가 아니므로 JUnit이 직접 발견 불가 → `@Suite`가 중간 다리 역할
- `@Suite` + `@IncludeEngines("cucumber")` + `@SelectClasspathResource` 조합으로 "이 클래스는 Cucumber 엔진을 통해 특정 `.feature` 파일을 실행하는 진입점"임을 선언

**`@ConfigurationParameter`의 역할:**

- `junit-platform.properties`의 설정을 어노테이션으로 클래스 단위에 전달
- 전역 설정(`properties`)과 달리 해당 클래스에만 적용됨

### 결과

**build.gradle 변경:**

- `junit-platform-suite` 의존성 추가

**Feature별 Runner 클래스 생성 (패키지: `gift.cucumber`):**

| 클래스                   | 대상 Feature                  |
|-----------------------|-----------------------------|
| `CategoryFeatureTest` | `features/category.feature` |
| `ProductFeatureTest`  | `features/product.feature`  |
| `GiftFeatureTest`     | `features/gift.feature`     |

**`junit-platform.properties` 변경:**

- `cucumber.features=classpath:features` 제거 — Suite Runner와 중복 실행 방지
- `cucumber.junit-platform.naming-strategy=long` 제거 — Suite 방식에서 불필요
- 유지: `cucumber.glue=gift`, `cucumber.plugin=pretty`

**참고 공식 문서:**

- [cucumber-jvm/cucumber-junit-platform-engine README](https://github.com/cucumber/cucumber-jvm/blob/main/cucumber-junit-platform-engine/README.md)
- [v7.0.0 Release Notes (`@Cucumber` → `@Suite` 마이그레이션)](https://github.com/cucumber/cucumber-jvm/blob/main/release-notes/v7.0.0.md)

**테스트 실행 결과:** 전체 통과 (BUILD SUCCESSFUL)
